package com.kafkaexample.ledger.messaging;

import com.kafkaexample.ledger.accounting.TransactionCreatedEvent;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.stereotype.Component;

@Component
public class TransactionEventParser {

	private final ObjectReader eventReader;

	/**
	 * A tolerant reader: unknown fields are ignored, so the producer can add fields without
	 * breaking this service. Every field this service does read is required.
	 */
	public TransactionEventParser(JsonMapper jsonMapper) {
		this.eventReader = jsonMapper.readerFor(TransactionCreatedEvent.class)
			.without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.with(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES);
	}

	public TransactionCreatedEvent parse(String json) {
		try {
			return eventReader.readValue(json);
		}
		catch (JacksonException | IllegalArgumentException exception) {
			throw new InvalidTransactionEventException("Not a valid TransactionCreated event: " + exception.getMessage(),
					exception);
		}
	}

}
