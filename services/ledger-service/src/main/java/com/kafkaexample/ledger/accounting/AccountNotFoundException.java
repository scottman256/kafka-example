package com.kafkaexample.ledger.accounting;

public class AccountNotFoundException extends RuntimeException {

	public AccountNotFoundException(String accountName) {
		super("No account exists for " + accountName + ".");
	}

}
