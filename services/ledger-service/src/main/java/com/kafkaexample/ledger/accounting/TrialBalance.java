package com.kafkaexample.ledger.accounting;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * The totals of every account's balance. Debit balances belong to parties that have paid out
 * more than they received, credit balances to the rest. Every entry debits and credits the
 * same amount, so the two totals match and the net is zero while the ledger is consistent.
 */
public record TrialBalance(long accountCount, BigDecimal totalDebits, BigDecimal totalCredits) {

	/** SQL sums over no rows are null, so an empty ledger arrives with null totals. */
	public TrialBalance {
		totalDebits = Objects.requireNonNullElse(totalDebits, BigDecimal.ZERO);
		totalCredits = Objects.requireNonNullElse(totalCredits, BigDecimal.ZERO);
	}

	public BigDecimal netBalance() {
		return totalCredits.subtract(totalDebits);
	}

	public boolean isBalanced() {
		return netBalance().signum() == 0;
	}

}
