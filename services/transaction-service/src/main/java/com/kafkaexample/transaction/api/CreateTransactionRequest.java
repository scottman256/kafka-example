package com.kafkaexample.transaction.api;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.kafkaexample.transaction.domain.NewTransaction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTransactionRequest(
		@NotNull LocalDate transactionDate,
		@NotBlank @Size(min = 2, max = 100) String payer,
		@NotBlank @Size(min = 2, max = 100) String payee,
		@NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
		@Size(max = 255) String memo,
		@NotBlank @KnownSourceBank String sourceBank) {

	/** Strips surrounding whitespace before validation runs, so the length rules apply to the real value. */
	public CreateTransactionRequest {
		payer = strip(payer);
		payee = strip(payee);
		memo = strip(memo);
		sourceBank = strip(sourceBank);
	}

	public NewTransaction toNewTransaction() {
		String memoOrNull = (memo == null || memo.isEmpty()) ? null : memo;
		return new NewTransaction(transactionDate, payer, payee, amount, memoOrNull, sourceBank);
	}

	private static String strip(String text) {
		return (text != null) ? text.strip() : null;
	}

}
