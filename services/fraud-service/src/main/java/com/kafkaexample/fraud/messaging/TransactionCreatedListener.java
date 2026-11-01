package com.kafkaexample.fraud.messaging;

import com.kafkaexample.fraud.screening.RecordPosition;
import com.kafkaexample.fraud.screening.ScreeningService;
import com.kafkaexample.fraud.screening.TransactionCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionCreatedListener {

	private final TransactionEventParser transactionEventParser;

	private final ScreeningService screeningService;

	public TransactionCreatedListener(TransactionEventParser transactionEventParser, ScreeningService screeningService) {
		this.transactionEventParser = transactionEventParser;
		this.screeningService = screeningService;
	}

	@KafkaListener(topics = "${app.kafka.transactions-topic}")
	public void onTransactionCreated(ConsumerRecord<String, String> consumerRecord) {
		TransactionCreatedEvent transaction = transactionEventParser.parse(consumerRecord.value());
		screeningService.screen(transaction, new RecordPosition(consumerRecord.partition(), consumerRecord.offset()));
	}

}
