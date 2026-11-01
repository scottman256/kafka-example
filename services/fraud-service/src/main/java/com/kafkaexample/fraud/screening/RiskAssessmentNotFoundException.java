package com.kafkaexample.fraud.screening;

import java.util.UUID;

public class RiskAssessmentNotFoundException extends RuntimeException {

	public RiskAssessmentNotFoundException(UUID transactionId) {
		super("No risk assessment exists for transaction " + transactionId + ".");
	}

}
