package com.kafkaexample.transaction.api;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.kafkaexample.transaction.domain.NewTransaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CreateTransactionRequestTest {

	private static final LocalDate TRANSACTION_DATE = LocalDate.of(2026, 10, 30);

	private static final BigDecimal AMOUNT = new BigDecimal("125.50");

	@Test
	void stripsSurroundingWhitespaceFromTextFields() {
		CreateTransactionRequest request = new CreateTransactionRequest(TRANSACTION_DATE, "  Alice Smith ",
				" Bob's Hardware\t", AMOUNT, " Lumber ", " First National Bank ");

		assertThat(request.payer()).isEqualTo("Alice Smith");
		assertThat(request.payee()).isEqualTo("Bob's Hardware");
		assertThat(request.memo()).isEqualTo("Lumber");
		assertThat(request.sourceBank()).isEqualTo("First National Bank");
	}

	@Test
	void toNewTransactionCopiesEveryField() {
		CreateTransactionRequest request = new CreateTransactionRequest(TRANSACTION_DATE, "Alice Smith",
				"Bob's Hardware", AMOUNT, "Lumber", "First National Bank");

		assertThat(request.toNewTransaction()).isEqualTo(new NewTransaction(TRANSACTION_DATE, "Alice Smith",
				"Bob's Hardware", AMOUNT, "Lumber", "First National Bank"));
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", "   " })
	void toNewTransactionTreatsAMissingOrBlankMemoAsNoMemo(String memo) {
		CreateTransactionRequest request = new CreateTransactionRequest(TRANSACTION_DATE, "Alice Smith",
				"Bob's Hardware", AMOUNT, memo, "First National Bank");

		assertThat(request.toNewTransaction().memo()).isNull();
	}

}
