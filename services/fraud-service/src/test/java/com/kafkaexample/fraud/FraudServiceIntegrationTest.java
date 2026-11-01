package com.kafkaexample.fraud;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

import com.kafkaexample.fraud.screening.RiskAssessment;
import com.kafkaexample.fraud.screening.RiskAssessmentRepository;
import com.kafkaexample.fraud.screening.RiskRule;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Runs the whole service against an embedded Kafka broker and an in-memory H2 database:
 * Kafka record → listener → screening → repository → database → REST API.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 3, topics = { FraudServiceIntegrationTest.TOPIC, FraudServiceIntegrationTest.DEAD_LETTER_TOPIC })
class FraudServiceIntegrationTest {

	static final String TOPIC = "transactions.created";

	static final String DEAD_LETTER_TOPIC = "transactions.created.fraud-service.DLT";

	private static final Duration SCREENING_TIMEOUT = Duration.ofSeconds(20);

	private static final Duration DEAD_LETTER_POLL = Duration.ofSeconds(3);

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	@Autowired
	private EmbeddedKafkaBroker embeddedKafka;

	@Autowired
	private RiskAssessmentRepository riskAssessmentRepository;

	@Autowired
	private MockMvcTester mockMvc;

	@Test
	void publishedTransactionIsScreenedAndServedByTheApi() {
		String payer = uniqueName("Mallory");
		UUID transactionId = publish(payer, payer, "9500.00", Instant.now());

		RiskAssessment assessment = awaitAssessment(transactionId);

		assertThat(assessment.getTriggeredRules()).containsExactlyInAnyOrder(RiskRule.SELF_PAYMENT,
				RiskRule.POSSIBLE_STRUCTURING, RiskRule.NEW_PAYEE_HIGH_VALUE);
		assertThat(mockMvc.get().uri("/api/risk/assessments/{id}", transactionId)).hasStatusOk()
			.bodyJson()
			.extractingPath("$.riskLevel")
			.isEqualTo("HIGH");
	}

	@Test
	void fourthPaymentWithinTenMinutesIsFlaggedForVelocity() {
		String payer = uniqueName("Victor");
		Instant start = Instant.now();
		for (int minute = 0; minute < 3; minute++) {
			publish(payer, "Coffee Shop", "4.50", start.plus(Duration.ofMinutes(minute)));
		}

		UUID fourthPayment = publish(payer, "Coffee Shop", "4.50", start.plus(Duration.ofMinutes(3)));

		assertThat(awaitAssessment(fourthPayment).getTriggeredRules()).containsExactly(RiskRule.HIGH_VELOCITY);
	}

	/**
	 * The later payment shares the duplicate's partition, so once it is screened the duplicate
	 * has been handled. Skipping it means no second row and nothing sent to the dead-letter topic.
	 */
	@Test
	void redeliveredEventIsScreenedOnlyOnce() {
		String payer = uniqueName("Rita");
		String event = eventJson(UUID.randomUUID(), UUID.randomUUID(), payer, "Grocer", "20.00", Instant.now());
		send(payer, event);
		send(payer, event);
		UUID laterPayment = publish(payer, "Grocer", "21.00", Instant.now());

		awaitAssessment(laterPayment);

		assertThat(riskAssessmentRepository.countPaymentsByPayerBetween(payer.toLowerCase(),
				Instant.now().minus(Duration.ofHours(1)), Instant.now().plus(Duration.ofHours(1))))
			.isEqualTo(2);
		assertThat(deadLettersWithKey(payer.toLowerCase())).isEmpty();
	}

	@Test
	void malformedRecordGoesStraightToTheDeadLetterTopic() {
		String key = uniqueName("poison").toLowerCase();
		send(key, "this is not json");

		ConsumerRecord<String, String> deadLetter = await().atMost(SCREENING_TIMEOUT)
			.until(() -> deadLettersWithKey(key), deadLetters -> !deadLetters.isEmpty())
			.get(0);

		assertThat(deadLetter.value()).isEqualTo("this is not json");
		assertThat(header(deadLetter, KafkaHeaders.DLT_EXCEPTION_CAUSE_FQCN))
			.isEqualTo("com.kafkaexample.fraud.messaging.InvalidTransactionEventException");
		assertThat(header(deadLetter, KafkaHeaders.DLT_ORIGINAL_TOPIC)).isEqualTo(TOPIC);
	}

	private UUID publish(String payer, String payee, String amount, Instant occurredAt) {
		UUID transactionId = UUID.randomUUID();
		send(payer, eventJson(UUID.randomUUID(), transactionId, payer, payee, amount, occurredAt));
		return transactionId;
	}

	private void send(String key, String value) {
		kafkaTemplate.send(TOPIC, key.toLowerCase(), value).join();
	}

	private RiskAssessment awaitAssessment(UUID transactionId) {
		return await().atMost(SCREENING_TIMEOUT)
			.until(() -> riskAssessmentRepository.findByTransactionId(transactionId), found -> found.isPresent())
			.orElseThrow();
	}

	private List<ConsumerRecord<String, String>> deadLettersWithKey(String key) {
		var consumerProperties = KafkaTestUtils.consumerProps(embeddedKafka, "dead-letter-reader-" + UUID.randomUUID(),
				false);
		try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(consumerProperties,
				new StringDeserializer(), new StringDeserializer()).createConsumer()) {
			embeddedKafka.consumeFromAnEmbeddedTopic(consumer, DEAD_LETTER_TOPIC);
			ConsumerRecords<String, String> deadLetters = KafkaTestUtils.getRecords(consumer, DEAD_LETTER_POLL);
			return StreamSupport.stream(deadLetters.spliterator(), false)
				.filter(deadLetter -> key.equals(deadLetter.key()))
				.toList();
		}
	}

	/** The JSON transaction-service publishes, including fields this service ignores. */
	private static String eventJson(UUID eventId, UUID transactionId, String payer, String payee, String amount,
			Instant occurredAt) {
		return """
				{"eventId":"%s","eventType":"TransactionCreated","eventVersion":1,"occurredAt":"%s",
				 "transactionId":"%s","transactionDate":"%s","payer":"%s","payee":"%s","amount":%s,
				 "currency":"USD","memo":"integration test","sourceBank":"First National Bank"}
				""".formatted(eventId, occurredAt, transactionId, occurredAt.toString().substring(0, 10), payer, payee,
				amount);
	}

	private static String uniqueName(String prefix) {
		return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
	}

	private static String header(ConsumerRecord<String, String> consumerRecord, String name) {
		return new String(consumerRecord.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
	}

}
