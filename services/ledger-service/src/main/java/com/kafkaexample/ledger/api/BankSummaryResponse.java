package com.kafkaexample.ledger.api;

import java.math.BigDecimal;

import com.kafkaexample.ledger.accounting.BankSummary;

public record BankSummaryResponse(String bankName, BigDecimal totalOutflow, int transactionCount) {

	static BankSummaryResponse from(BankSummary bankSummary) {
		return new BankSummaryResponse(bankSummary.getBankName(), bankSummary.getTotalOutflow(),
				bankSummary.getTransactionCount());
	}

}
