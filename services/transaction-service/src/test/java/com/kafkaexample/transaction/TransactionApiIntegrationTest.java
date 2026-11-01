package com.kafkaexample.transaction;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.kafkaexample.transaction.api.TransactionResponse;
import com.kafkaexample.transaction.domain.TransactionRepository;
import com.kafkaexample.transaction.publishing.KafkaPublishLogEntry;
import com.kafkaexample.transaction.publishing.KafkaPublishLogRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the whole service against an in-memory H2 database and an embedded Kafka broker:
 * HTTP request → controller → service → repositories → database, plus the Kafka topic.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 3, topics = TransactionFixtures.TOPIC)
class TransactionApiIntegrationTest {

	@Autowired
	private MockMvcTester mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private EmbeddedKafkaBroker embeddedKafka;

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private KafkaPublishLogRepository kafkaPublishLogRepository;

	@Test
	void postedTransactionIsStoredAndPublishedWithTheLoggedKafkaMetadata() {
		TransactionResponse created = createTransaction();
		KafkaPublishLogEntry publishLogEntry = kafkaPublishLogRepository.findAll().stream()
			.filter(entry -> entry.getTransactionId().equals(created.id()))
			.findFirst()
			.orElseThrow();

		ConsumerRecord<String, String> publishedRecord = readRecordAt(publishLogEntry.getKafkaPartition(),
				publishLogEntry.getKafkaOffset());

		assertThat(transactionRepository.existsById(created.id())).isTrue();
		assertThat(publishedRecord.key()).isEqualTo("alice smith");
		assertThat(publishedRecord.value()).isEqualTo(publishLogEntry.getPayload());
		assertThat(correlationId(publishedRecord)).isEqualTo(created.id().toString());
	}

	@Test
	void createdTransactionAppearsFirstInTheRecentTransactions() {
		TransactionResponse created = createTransaction();

		assertThat(mockMvc.get().uri("/api/transactions")).hasStatusOk()
			.bodyJson()
			.extractingPath("$[0].id")
			.isEqualTo(created.id().toString());
	}

	@Test
	void invalidTransactionIsNeitherStoredNorPublished() {
		long transactionsBefore = transactionRepository.count();

		assertThat(post(TransactionFixtures.CREATE_REQUEST_JSON.replace("125.50", "-1"))).hasStatus(HttpStatus.BAD_REQUEST);

		assertThat(transactionRepository.count()).isEqualTo(transactionsBefore);
	}

	private TransactionResponse createTransaction() {
		MvcTestResult result = post(TransactionFixtures.CREATE_REQUEST_JSON);
		assertThat(result).hasStatus(HttpStatus.CREATED);
		return jsonMapper.readValue(result.getResponse().getContentAsByteArray(), TransactionResponse.class);
	}

	private MvcTestResult post(String json) {
		return mockMvc.post().uri("/api/transactions").contentType(MediaType.APPLICATION_JSON).content(json).exchange();
	}

	private ConsumerRecord<String, String> readRecordAt(int partition, long offset) {
		TopicPartition topicPartition = new TopicPartition(TransactionFixtures.TOPIC, partition);
		try (Consumer<String, String> consumer = createConsumer()) {
			consumer.assign(List.of(topicPartition));
			consumer.seek(topicPartition, offset);
			return KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10)).records(topicPartition).get(0);
		}
	}

	private Consumer<String, String> createConsumer() {
		Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps(embeddedKafka,
				"integration-test-" + UUID.randomUUID(), false);
		return new DefaultKafkaConsumerFactory<>(consumerProperties, new StringDeserializer(), new StringDeserializer())
			.createConsumer();
	}

	private static String correlationId(ConsumerRecord<String, String> consumerRecord) {
		return new String(consumerRecord.headers().lastHeader("correlationId").value(), StandardCharsets.UTF_8);
	}

}
