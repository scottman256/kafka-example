package com.kafkaexample.ledger.accounting;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrialBalanceTest {

	@Test
	void isBalancedWhenDebitAndCreditTotalsMatch() {
		TrialBalance trialBalance = new TrialBalance(3, new BigDecimal("150.00"), new BigDecimal("150.00"));

		assertThat(trialBalance.netBalance()).isZero();
		assertThat(trialBalance.isBalanced()).isTrue();
	}

	@Test
	void isNotBalancedWhenTheTotalsDiffer() {
		TrialBalance trialBalance = new TrialBalance(2, new BigDecimal("150.00"), new BigDecimal("125.50"));

		assertThat(trialBalance.netBalance()).isEqualByComparingTo("-24.50");
		assertThat(trialBalance.isBalanced()).isFalse();
	}

	@Test
	void anEmptyLedgerHasZeroTotals() {
		TrialBalance trialBalance = new TrialBalance(0, null, null);

		assertThat(trialBalance.totalDebits()).isZero();
		assertThat(trialBalance.totalCredits()).isZero();
		assertThat(trialBalance.isBalanced()).isTrue();
	}

}
