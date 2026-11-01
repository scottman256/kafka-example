package com.kafkaexample.fraud.screening;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PartyNameTest {

	@Test
	void normalizeIgnoresCaseAndSurroundingWhitespace() {
		assertThat(PartyName.normalize("  Alice SMITH\t")).isEqualTo("alice smith");
	}

}
