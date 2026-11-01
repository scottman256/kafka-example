package com.kafkaexample.ledger.accounting;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BankSummaryTest {

	private final BankSummary bankSummary = new BankSummary("First National Bank");

	@Test
	void startsWithNoOutflow() {
		assertThat(bankSummary.getBankName()).isEqualTo("First National Bank");
		assertThat(bankSummary.getTotalOutflow()).isZero();
		assertThat(bankSummary.getTransactionCount()).isZero();
	}

	@Test
	void recordOutflowAddsTheAmountAndCountsThePayment() {
		bankSummary.recordOutflow(new BigDecimal("125.50"));
		bankSummary.recordOutflow(new BigDecimal("74.50"));

		assertThat(bankSummary.getTotalOutflow()).isEqualByComparingTo("200.00");
		assertThat(bankSummary.getTransactionCount()).isEqualTo(2);
	}

}
