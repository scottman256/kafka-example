package com.kafkaexample.ledger.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.kafkaexample.ledger.accounting.EntrySide;
import com.kafkaexample.ledger.accounting.JournalEntry;
import com.kafkaexample.ledger.accounting.JournalLine;

public record JournalLineResponse(
		UUID transactionId,
		LocalDate transactionDate,
		EntrySide side,
		BigDecimal amount,
		String counterparty,
		String sourceBank,
		KafkaPositionResponse kafka,
		Instant postedAt) {

	static JournalLineResponse from(JournalLine line) {
		JournalEntry entry = line.getJournalEntry();
		return new JournalLineResponse(entry.getTransactionId(), entry.getTransactionDate(), line.getSide(),
				line.getAmount(), line.getCounterparty(), entry.getSourceBank(),
				new KafkaPositionResponse(entry.getKafkaPartition(), entry.getKafkaOffset()), entry.getPostedAt());
	}

}
