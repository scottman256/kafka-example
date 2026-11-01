package com.kafkaexample.ledger.api;

import java.util.List;

import com.kafkaexample.ledger.accounting.EntrySide;
import com.kafkaexample.ledger.accounting.JournalLine;
import com.kafkaexample.ledger.accounting.TransactionCreatedEvent;
import org.junit.jupiter.api.Test;

import static com.kafkaexample.ledger.LedgerFixtures.POSTED_AT;
import static com.kafkaexample.ledger.LedgerFixtures.RECORD_POSITION;
import static com.kafkaexample.ledger.LedgerFixtures.SOURCE_BANK;
import static com.kafkaexample.ledger.LedgerFixtures.TRANSACTION_DATE;
import static com.kafkaexample.ledger.LedgerFixtures.payment;
import static com.kafkaexample.ledger.LedgerFixtures.postedEntry;
import static org.assertj.core.api.Assertions.assertThat;

class JournalLineResponseTest {

	private final TransactionCreatedEvent transaction = payment();

	private final List<JournalLine> lines = postedEntry(transaction).getLines();

	@Test
	void fromDescribesThePayersDebit() {
		JournalLineResponse response = JournalLineResponse.from(lines.get(0));

		assertThat(response.transactionId()).isEqualTo(transaction.transactionId());
		assertThat(response.transactionDate()).isEqualTo(TRANSACTION_DATE);
		assertThat(response.side()).isEqualTo(EntrySide.DEBIT);
		assertThat(response.amount()).isEqualByComparingTo("-125.50");
		assertThat(response.counterparty()).isEqualTo("Bob's Hardware");
		assertThat(response.sourceBank()).isEqualTo(SOURCE_BANK);
		assertThat(response.kafka()).isEqualTo(new KafkaPositionResponse(RECORD_POSITION.partition(), RECORD_POSITION.offset()));
		assertThat(response.postedAt()).isEqualTo(POSTED_AT);
	}

	@Test
	void fromDescribesThePayeesCredit() {
		JournalLineResponse response = JournalLineResponse.from(lines.get(1));

		assertThat(response.side()).isEqualTo(EntrySide.CREDIT);
		assertThat(response.amount()).isEqualByComparingTo("125.50");
		assertThat(response.counterparty()).isEqualTo("Alice Smith");
	}

}
