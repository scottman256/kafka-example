package com.kafkaexample.transaction.domain;

import java.util.List;

public final class SourceBanks {

	public static final List<String> ALL = List.of(
			"First National Bank",
			"Summit Credit Union",
			"Harbor Savings Bank",
			"Pioneer Trust",
			"Evergreen Community Bank",
			"Metro Federal Bank");

	private SourceBanks() {
	}

	public static boolean isKnown(String bankName) {
		return ALL.contains(bankName);
	}

}
