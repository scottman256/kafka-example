package com.kafkaexample.ledger.messaging;

import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

/**
 * Retries a failed record a few times with growing pauses, then moves it to this service's
 * dead-letter topic so one bad record never blocks its partition.
 */
@Configuration(proxyBeanMethods = false)
public class KafkaErrorHandlingConfiguration {

	static final int MAX_RETRIES = 3;

	static final long INITIAL_RETRY_INTERVAL_MILLIS = 500;

	private static final Logger logger = LoggerFactory.getLogger(KafkaErrorHandlingConfiguration.class);

	@Bean
	public DefaultErrorHandler kafkaErrorHandler(KafkaOperations<String, String> kafkaOperations,
			@Value("${app.kafka.dead-letter-topic}") String deadLetterTopic) {
		DefaultErrorHandler errorHandler = new DefaultErrorHandler(deadLetterRecoverer(kafkaOperations, deadLetterTopic),
				retryBackOff());
		errorHandler.addNotRetryableExceptions(InvalidTransactionEventException.class);
		return errorHandler;
	}

	/**
	 * The dead-letter topic has as many partitions as the source, so each record keeps its
	 * partition number. Spring publishes dead letters silently, so each one is logged here.
	 */
	private static ConsumerRecordRecoverer deadLetterRecoverer(KafkaOperations<String, String> kafkaOperations,
			String deadLetterTopic) {
		DeadLetterPublishingRecoverer deadLetterPublisher = new DeadLetterPublishingRecoverer(kafkaOperations,
				(failedRecord, exception) -> new TopicPartition(deadLetterTopic, failedRecord.partition()));
		return (failedRecord, exception) -> {
			logger.warn("Moving {}-{}@{} to {}: {}", failedRecord.topic(), failedRecord.partition(),
					failedRecord.offset(), deadLetterTopic, NestedExceptionUtils.getMostSpecificCause(exception).getMessage());
			deadLetterPublisher.accept(failedRecord, exception);
		};
	}

	private static ExponentialBackOffWithMaxRetries retryBackOff() {
		ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(MAX_RETRIES);
		backOff.setInitialInterval(INITIAL_RETRY_INTERVAL_MILLIS);
		backOff.setMultiplier(2.0);
		return backOff;
	}

}
