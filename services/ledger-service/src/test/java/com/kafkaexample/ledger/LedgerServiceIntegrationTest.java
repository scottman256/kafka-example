package com.kafkaexample.ledger;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import com.kafkaexample.ledger.accounting.Account;
import com.kafkaexample.ledger.accounting.AccountRepository;
import com.kafkaexample.ledger.accounting.JournalEntryRepository;
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
 * Kafka record → listener → posting → repositories → database → REST API.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 3, topics = { LedgerServiceIntegrationTest.TOPIC, LedgerServiceIntegrationTest.DEAD_LETTER_TOPIC })
class LedgerServiceIntegrationTest {

	static final String TOPIC = "transactions.created";

	static final String DEAD_LETTER_TOPIC = "transactions.created.ledger-service.DLT";

	private static final Duration POSTING_TIMEOUT = Duration.ofSeconds(30);

	private static final Duration DEAD_LETTER_POLL = Duration.ofSeconds(3);

	private static final int CONCURRENT_PAYERS = 30;

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	@Autowired
	private EmbeddedKafkaBroker embeddedKafka;

	@Autowired
	private JournalEntryRepository journalEntryRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private MockMvcTester mockMvc;

	@Test
	void publishedTransactionMovesBothBalancesAndKeepsTheLedgerBalanced() {
		String payer = uniqueName("Alice");
		String payee = uniqueName("Hardware Store");

		awaitPosted(publish(payer, payee, "125.50"));

		assertThat(account(payer).getBalance()).isEqualByComparingTo("-125.50");
		assertThat(mockMvc.get().uri("/api/ledger/accounts/{name}/entries", payee)).hasStatusOk()
			.bodyJson()
			.hasPathSatisfying("$[0].side", side -> assertThat(side).asString().isEqualTo("CREDIT"))
			.hasPathSatisfying("$[0].counterparty", party -> assertThat(party).asString().isEqualTo(payer));
		assertThat(mockMvc.get().uri("/api/ledger/trial-balance")).hasStatusOk()
			.bodyJson()
			.extractingPath("$.balanced")
			.isEqualTo(true);
	}

	/**
	 * The later payment shares the duplicate's partition, so once it is posted the duplicate
	 * has been handled. Skipping it means one posting and nothing sent to the dead-letter topic.
	 */
	@Test
	void redeliveredEventIsPostedOnlyOnce() {
		String payer = uniqueName("Rita");
		String payee = uniqueName("Grocer");
		String event = eventJson(UUID.randomUUID(), payer, payee, "20.00");
		send(payer, event);
		send(payer, event);

		awaitPosted(publish(payer, payee, "1.00"));

		assertThat(account(payee).getBalance()).isEqualByComparingTo("21.00");
		assertThat(deadLettersWithKey(Account.normalizeName(payer))).isEmpty();
	}

	/**
	 * Each payer has its own key, so the payments land on all three partitions and are posted
	 * by concurrent listener threads that all update the one payee account and the one bank.
	 */
	@Test
	void concurrentPaymentsToOnePayeeAreAllCounted() {
		String payee = uniqueName("Charity");
		List<UUID> eventIds = IntStream.range(0, CONCURRENT_PAYERS)
			.mapToObj(payerNumber -> publish(uniqueName("Donor " + payerNumber), payee, "1.00"))
			.toList();

		eventIds.forEach(this::awaitPosted);

		assertThat(account(payee).getBalance()).isEqualByComparingTo(new BigDecimal(CONCURRENT_PAYERS));
		assertThat(account(payee).getEntryCount()).isEqualTo(CONCURRENT_PAYERS);
		assertThat(accountRepository.calculateTrialBalance().isBalanced()).isTrue();
	}

	@Test
	void malformedRecordGoesStraightToTheDeadLetterTopic() {
		String key = Account.normalizeName(uniqueName("poison"));
		send(key, "this is not json");

		ConsumerRecord<String, String> deadLetter = await().atMost(POSTING_TIMEOUT)
			.until(() -> deadLettersWithKey(key), deadLetters -> !deadLetters.isEmpty())
			.get(0);

		assertThat(deadLetter.value()).isEqualTo("this is not json");
		assertThat(header(deadLetter, KafkaHeaders.DLT_EXCEPTION_CAUSE_FQCN))
			.isEqualTo("com.kafkaexample.ledger.messaging.InvalidTransactionEventException");
		assertThat(header(deadLetter, KafkaHeaders.DLT_ORIGINAL_TOPIC)).isEqualTo(TOPIC);
	}

	private UUID publish(String payer, String payee, String amount) {
		UUID eventId = UUID.randomUUID();
		send(payer, eventJson(eventId, payer, payee, amount));
		return eventId;
	}

	private void send(String key, String value) {
		kafkaTemplate.send(TOPIC, Account.normalizeName(key), value).join();
	}

	private void awaitPosted(UUID eventId) {
		await().atMost(POSTING_TIMEOUT).until(() -> journalEntryRepository.existsByEventId(eventId));
	}

	private Account account(String name) {
		return accountRepository.findByNormalizedName(Account.normalizeName(name)).orElseThrow();
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
	private static String eventJson(UUID eventId, String payer, String payee, String amount) {
		return """
				{"eventId":"%s","eventType":"TransactionCreated","eventVersion":1,"occurredAt":"%s",
				 "transactionId":"%s","transactionDate":"2026-10-30","payer":"%s","payee":"%s","amount":%s,
				 "currency":"USD","memo":"integration test","sourceBank":"First National Bank"}
				""".formatted(eventId, Instant.now(), UUID.randomUUID(), payer, payee, amount);
	}

	private static String uniqueName(String prefix) {
		return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
	}

	private static String header(ConsumerRecord<String, String> consumerRecord, String name) {
		return new String(consumerRecord.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
	}

}
