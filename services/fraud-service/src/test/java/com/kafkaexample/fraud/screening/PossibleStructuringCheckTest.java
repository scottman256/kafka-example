package com.kafkaexample.fraud.screening;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.kafkaexample.fraud.ScreeningFixtures.paymentOf;
import static org.assertj.core.api.Assertions.assertThat;

class PossibleStructuringCheckTest {

	private final PossibleStructuringCheck check = new PossibleStructuringCheck();

	@ParameterizedTest
	@CsvSource({ "8999.99, false", "9000.00, true", "9999.99, true", "10000.00, false" })
	void triggersJustUnderTheReportingThreshold(String amount, boolean triggered) {
		assertThat(check.isTriggeredBy(paymentOf(amount))).isEqualTo(triggered);
	}

}
