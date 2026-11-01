package com.kafkaexample.transaction.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.kafkaexample.transaction.domain.Transaction;
import com.kafkaexample.transaction.domain.TransactionWithPublishLog;

public record TransactionResponse(
		UUID id,
		LocalDate transactionDate,
		String payer,
		String payee,
		BigDecimal amount,
		String currency,
		String memo,
		String sourceBank,
		Instant createdAt,
		KafkaPublicationResponse kafka) {

	static TransactionResponse from(TransactionWithPublishLog transactionWithPublishLog) {
		Transaction transaction = transactionWithPublishLog.transaction();
		return new TransactionResponse(transaction.getId(), transaction.getTransactionDate(), transaction.getPayer(),
				transaction.getPayee(), transaction.getAmount(), transaction.getCurrency(), transaction.getMemo(),
				transaction.getSourceBank(), transaction.getCreatedAt(), kafkaPublication(transactionWithPublishLog));
	}

	private static KafkaPublicationResponse kafkaPublication(TransactionWithPublishLog transactionWithPublishLog) {
		return (transactionWithPublishLog.publishLogEntry() != null)
				? KafkaPublicationResponse.from(transactionWithPublishLog.publishLogEntry()) : null;
	}

}
