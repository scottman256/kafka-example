package com.kafkaexample.fraud.screening;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.kafkaexample.fraud.ScreeningFixtures.paymentOf;
import static org.assertj.core.api.Assertions.assertThat;

class LargeAmountCheckTest {

	private final LargeAmountCheck check = new LargeAmountCheck();

	@ParameterizedTest
	@CsvSource({ "9999.99, false", "10000.00, true", "25000.00, true" })
	void triggersAtTheReportingThreshold(String amount, boolean triggered) {
		assertThat(check.isTriggeredBy(paymentOf(amount))).isEqualTo(triggered);
	}

}
