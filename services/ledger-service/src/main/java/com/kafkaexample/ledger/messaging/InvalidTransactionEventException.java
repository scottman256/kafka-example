package com.kafkaexample.ledger.messaging;

/** A record that can never be posted. Retrying would not help, so it goes straight to the dead-letter topic. */
public class InvalidTransactionEventException extends RuntimeException {

	public InvalidTransactionEventException(String message, Throwable cause) {
		super(message, cause);
	}

}
