package com.kafkaexample.fraud.screening;

import java.util.Locale;

final class PartyName {

	private PartyName() {
	}

	static String normalize(String name) {
		return name.strip().toLowerCase(Locale.ROOT);
	}

}
