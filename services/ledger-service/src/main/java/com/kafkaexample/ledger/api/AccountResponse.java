package com.kafkaexample.ledger.api;

import java.math.BigDecimal;
import java.time.Instant;

import com.kafkaexample.ledger.accounting.Account;

public record AccountResponse(String name, BigDecimal balance, int entryCount, Instant openedAt) {

	static AccountResponse from(Account account) {
		return new AccountResponse(account.getName(), account.getBalance(), account.getEntryCount(),
				account.getOpenedAt());
	}

}
