package com.kafkaexample.ledger.api;

import java.math.BigDecimal;

import com.kafkaexample.ledger.accounting.TrialBalance;

public record TrialBalanceResponse(
		long accountCount,
		BigDecimal totalDebits,
		BigDecimal totalCredits,
		BigDecimal netBalance,
		boolean balanced) {

	static TrialBalanceResponse from(TrialBalance trialBalance) {
		return new TrialBalanceResponse(trialBalance.accountCount(), trialBalance.totalDebits(),
				trialBalance.totalCredits(), trialBalance.netBalance(), trialBalance.isBalanced());
	}

}
