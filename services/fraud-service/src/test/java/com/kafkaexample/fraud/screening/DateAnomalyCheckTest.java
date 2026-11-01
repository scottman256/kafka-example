package com.kafkaexample.fraud.screening;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.kafkaexample.fraud.ScreeningFixtures.paymentDated;
import static org.assertj.core.api.Assertions.assertThat;

class DateAnomalyCheckTest {

	private final DateAnomalyCheck check = new DateAnomalyCheck();

	/** The fixture event occurred on 2026-10-31 (UTC). */
	@ParameterizedTest
	@CsvSource({
			"2026-10-31, false", // same day
			"2026-11-01, false", // one day ahead: time-zone grace
			"2026-11-02, true", // in the future
			"2026-08-02, false", // exactly 90 days old
			"2026-08-01, true" // older than 90 days
	})
	void triggersForFutureOrStaleTransactionDates(LocalDate transactionDate, boolean triggered) {
		assertThat(check.isTriggeredBy(paymentDated(transactionDate))).isEqualTo(triggered);
	}

	@Test
	void reportsTheDateAnomalyRule() {
		assertThat(check.rule()).isEqualTo(RiskRule.DATE_ANOMALY);
	}

}
