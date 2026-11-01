package com.kafkaexample.ledger.messaging;

import com.kafkaexample.ledger.accounting.PostingService;
import com.kafkaexample.ledger.accounting.RecordPosition;
import com.kafkaexample.ledger.accounting.TransactionCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionCreatedListener {

	private final TransactionEventParser transactionEventParser;

	private final PostingService postingService;

	@KafkaListener(topics = "${app.kafka.transactions-topic}")
	public void onTransactionCreated(ConsumerRecord<String, String> consumerRecord) {
		TransactionCreatedEvent transaction = transactionEventParser.parse(consumerRecord.value());
		postingService.post(transaction, new RecordPosition(consumerRecord.partition(), consumerRecord.offset()));
	}

}
