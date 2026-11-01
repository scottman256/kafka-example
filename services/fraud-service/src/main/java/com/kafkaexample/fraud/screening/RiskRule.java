package com.kafkaexample.fraud.screening;

public enum RiskRule {

	LARGE_AMOUNT(40, "Amount is $10,000 or more, the Currency Transaction Report threshold"),
	POSSIBLE_STRUCTURING(30, "Amount is just under the $10,000 reporting threshold"),
	HIGH_VELOCITY(30, "Payer made 3 or more other payments in the previous 10 minutes"),
	SELF_PAYMENT(50, "Payer and payee are the same party"),
	NEW_PAYEE_HIGH_VALUE(15, "First payment from this payer to this payee, and over $1,000"),
	DATE_ANOMALY(20, "Transaction date is in the future or more than 90 days old");

	private final int points;

	private final String description;

	RiskRule(int points, String description) {
		this.points = points;
		this.description = description;
	}

	public int points() {
		return points;
	}

	public String description() {
		return description;
	}

}
