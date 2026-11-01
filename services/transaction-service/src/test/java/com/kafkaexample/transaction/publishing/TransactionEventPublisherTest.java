package com.kafkaexample.transaction.publishing;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.support.SendResult;

import static com.kafkaexample.transaction.TransactionFixtures.CREATED_AT;
import static com.kafkaexample.transaction.TransactionFixtures.OFFSET;
import static com.kafkaexample.transaction.TransactionFixtures.PARTITION;
import static com.kafkaexample.transaction.TransactionFixtures.TOPIC;
import static com.kafkaexample.transaction.TransactionFixtures.TRANSACTION_ID;
import static com.kafkaexample.transaction.TransactionFixtures.recordMetadata;
import static com.kafkaexample.transaction.TransactionFixtures.savedTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionEventPublisherTest {

	@Mock
	private KafkaOperations<String, String> kafkaOperations;

	@Mock
	private KafkaPublishLogRepository kafkaPublishLogRepository;

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	private final TransactionCreatedEvent event = savedTransaction().toCreatedEvent(CREATED_AT);

	private TransactionEventPublisher publisher;

	@BeforeEach
	void createPublisher() {
		Clock clock = Clock.fixed(CREATED_AT, ZoneOffset.UTC);
		publisher = new TransactionEventPublisher(kafkaOperations, kafkaPublishLogRepository, jsonMapper, clock, TOPIC);
		lenient().when(kafkaPublishLogRepository.save(any(KafkaPublishLogEntry.class)))
			.thenAnswer(call -> call.getArgument(0));
	}

	@Test
	void publishSendsToTheTopicKeyedByTheNormalisedPayer() {
		brokerAccepts();

		publisher.publish(event);

		assertThat(sentRecord().topic()).isEqualTo(TOPIC);
		assertThat(sentRecord().key()).isEqualTo("alice smith");
	}

	@Test
	void publishAddsEventTypeVersionAndCorrelationHeaders() {
		brokerAccepts();

		publisher.publish(event);

		assertThat(header(TransactionEventPublisher.EVENT_TYPE_HEADER)).isEqualTo("TransactionCreated");
		assertThat(header(TransactionEventPublisher.EVENT_VERSION_HEADER)).isEqualTo("1");
		assertThat(header(TransactionEventPublisher.CORRELATION_ID_HEADER)).isEqualTo(TRANSACTION_ID.toString());
	}

	@Test
	void publishSendsTheEventAsJson() {
		brokerAccepts();

		publisher.publish(event);

		JsonNode payload = jsonMapper.readTree(sentRecord().value());
		assertThat(payload.get("eventId").asString()).isEqualTo(event.eventId().toString());
		assertThat(payload.get("transactionDate").asString()).isEqualTo("2026-10-30");
		assertThat(payload.get("amount").decimalValue()).isEqualByComparingTo("125.50");
	}

	@Test
	void publishLogsThePayloadAndBrokerAcknowledgement() {
		brokerAccepts();

		KafkaPublishLogEntry publishLogEntry = publisher.publish(event);

		assertThat(publishLogEntry.isPublished()).isTrue();
		assertThat(publishLogEntry.getPayload()).isEqualTo(sentRecord().value());
		assertThat(publishLogEntry.getKafkaPartition()).isEqualTo(PARTITION);
		assertThat(publishLogEntry.getKafkaOffset()).isEqualTo(OFFSET);
		verify(kafkaPublishLogRepository).save(publishLogEntry);
	}

	@Test
	void publishLogsAFailureWhenTheBrokerRejectsTheRecord() {
		given(kafkaOperations.send(anyProducerRecord()))
			.willReturn(CompletableFuture.failedFuture(new TimeoutException("Expiring 1 record(s)")));

		KafkaPublishLogEntry publishLogEntry = publisher.publish(event);

		assertThat(publishLogEntry.getStatus()).isEqualTo(PublishStatus.FAILED);
		assertThat(publishLogEntry.getErrorMessage()).contains("TimeoutException", "Expiring 1 record(s)");
		verify(kafkaPublishLogRepository).save(publishLogEntry);
	}

	@Test
	void publishLogsAFailureWhenSendingFailsImmediately() {
		given(kafkaOperations.send(anyProducerRecord()))
			.willThrow(new KafkaException("Send failed", new TimeoutException("Topic not present in metadata")));

		KafkaPublishLogEntry publishLogEntry = publisher.publish(event);

		assertThat(publishLogEntry.getStatus()).isEqualTo(PublishStatus.FAILED);
		assertThat(publishLogEntry.getErrorMessage()).contains("Topic not present in metadata");
	}

	@Test
	void partitionKeyIgnoresCaseAndSurroundingWhitespace() {
		assertThat(TransactionEventPublisher.partitionKey("  Alice SMITH ")).isEqualTo("alice smith");
	}

	private void brokerAccepts() {
		given(kafkaOperations.send(anyProducerRecord())).willAnswer(call -> CompletableFuture
			.completedFuture(new SendResult<>(call.<ProducerRecord<String, String>>getArgument(0), recordMetadata())));
	}

	private static ProducerRecord<String, String> anyProducerRecord() {
		return any();
	}

	@SuppressWarnings("unchecked")
	private ProducerRecord<String, String> sentRecord() {
		ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
		verify(kafkaOperations).send(recordCaptor.capture());
		return recordCaptor.getValue();
	}

	private String header(String name) {
		return new String(sentRecord().headers().lastHeader(name).value(), StandardCharsets.UTF_8);
	}

}
