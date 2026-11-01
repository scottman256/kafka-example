package com.kafkaexample.ledger.accounting;

import java.util.List;

import org.junit.jupiter.api.Test;

import static com.kafkaexample.ledger.LedgerFixtures.payment;
import static com.kafkaexample.ledger.LedgerFixtures.postedEntry;
import static org.assertj.core.api.Assertions.assertThat;

class JournalLineTest {

	private final List<JournalLine> lines = postedEntry(payment()).getLines();

	@Test
	void thePayersLineIsADebitAgainstThePayee() {
		JournalLine payerLine = lines.get(0);

		assertThat(payerLine.getSide()).isEqualTo(EntrySide.DEBIT);
		assertThat(payerLine.getAmount()).isEqualByComparingTo("-125.50");
		assertThat(payerLine.getCounterparty()).isEqualTo("Bob's Hardware");
	}

	@Test
	void thePayeesLineIsACreditFromThePayer() {
		JournalLine payeeLine = lines.get(1);

		assertThat(payeeLine.getSide()).isEqualTo(EntrySide.CREDIT);
		assertThat(payeeLine.getAmount()).isEqualByComparingTo("125.50");
		assertThat(payeeLine.getCounterparty()).isEqualTo("Alice Smith");
	}

}
