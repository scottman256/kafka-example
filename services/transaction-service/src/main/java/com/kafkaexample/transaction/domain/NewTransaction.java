package com.kafkaexample.transaction.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NewTransaction(
		LocalDate transactionDate,
		String payer,
		String payee,
		BigDecimal amount,
		String memo,
		String sourceBank) {
}
