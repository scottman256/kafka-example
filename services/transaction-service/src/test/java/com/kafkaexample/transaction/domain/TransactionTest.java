package com.kafkaexample.transaction.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.kafkaexample.transaction.publishing.TransactionCreatedEvent;
import org.junit.jupiter.api.Test;

import static com.kafkaexample.transaction.TransactionFixtures.CREATED_AT;
import static com.kafkaexample.transaction.TransactionFixtures.TRANSACTION_ID;
import static com.kafkaexample.transaction.TransactionFixtures.newTransaction;
import static com.kafkaexample.transaction.TransactionFixtures.savedTransaction;
import static org.assertj.core.api.Assertions.assertThat;

class TransactionTest {

	@Test
	void createCopiesTheDetailsAndStampsCurrencyAndCreationTime() {
		Transaction transaction = Transaction.create(newTransaction(), CREATED_AT);

		assertThat(transaction.getTransactionDate()).isEqualTo(LocalDate.of(2026, 10, 30));
		assertThat(transaction.getPayer()).isEqualTo("Alice Smith");
		assertThat(transaction.getPayee()).isEqualTo("Bob's Hardware");
		assertThat(transaction.getMemo()).isEqualTo("Lumber for deck");
		assertThat(transaction.getSourceBank()).isEqualTo("First National Bank");
		assertThat(transaction.getCurrency()).isEqualTo("USD");
		assertThat(transaction.getCreatedAt()).isEqualTo(CREATED_AT);
	}

	@Test
	void createStoresTheAmountWithTwoDecimalPlaces() {
		NewTransaction wholeDollars = new NewTransaction(LocalDate.of(2026, 10, 30), "Alice Smith", "Bob's Hardware",
				new BigDecimal("125.5"), null, "First National Bank");

		assertThat(Transaction.create(wholeDollars, CREATED_AT).getAmount()).hasToString("125.50");
	}

	@Test
	void toCreatedEventCarriesTheTransactionDetails() {
		TransactionCreatedEvent event = savedTransaction().toCreatedEvent(CREATED_AT);

		assertThat(event.eventType()).isEqualTo("TransactionCreated");
		assertThat(event.eventVersion()).isEqualTo(1);
		assertThat(event.occurredAt()).isEqualTo(CREATED_AT);
		assertThat(event.transactionId()).isEqualTo(TRANSACTION_ID);
		assertThat(event.payer()).isEqualTo("Alice Smith");
		assertThat(event.payee()).isEqualTo("Bob's Hardware");
		assertThat(event.amount()).isEqualByComparingTo("125.50");
		assertThat(event.currency()).isEqualTo("USD");
		assertThat(event.sourceBank()).isEqualTo("First National Bank");
	}

	@Test
	void toCreatedEventGivesEveryEventItsOwnId() {
		Transaction transaction = savedTransaction();

		assertThat(transaction.toCreatedEvent(CREATED_AT).eventId())
			.isNotNull()
			.isNotEqualTo(transaction.toCreatedEvent(CREATED_AT).eventId());
	}

}
