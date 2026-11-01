package com.kafkaexample.transaction.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KnownSourceBankValidatorTest {

	private final KnownSourceBankValidator validator = new KnownSourceBankValidator();

	@Test
	void acceptsASupportedBank() {
		assertThat(validator.isValid("Summit Credit Union", null)).isTrue();
	}

	@Test
	void rejectsAnUnknownBank() {
		assertThat(validator.isValid("Bank of Nowhere", null)).isFalse();
	}

	@Test
	void rejectsASupportedBankWithDifferentCase() {
		assertThat(validator.isValid("summit credit union", null)).isFalse();
	}

	@Test
	void leavesAMissingBankToTheNotBlankRule() {
		assertThat(validator.isValid(null, null)).isTrue();
	}

}
