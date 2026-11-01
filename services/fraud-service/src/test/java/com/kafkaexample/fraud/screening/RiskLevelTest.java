package com.kafkaexample.fraud.screening;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class RiskLevelTest {

	@ParameterizedTest
	@CsvSource({ "0, LOW", "29, LOW", "30, MEDIUM", "69, MEDIUM", "70, HIGH", "100, HIGH" })
	void forScoreUsesTheLevelBoundaries(int score, RiskLevel expectedLevel) {
		assertThat(RiskLevel.forScore(score)).isEqualTo(expectedLevel);
	}

	@Test
	void mediumAndHighAreFlagged() {
		assertThat(RiskLevel.LOW.isFlagged()).isFalse();
		assertThat(RiskLevel.MEDIUM.isFlagged()).isTrue();
		assertThat(RiskLevel.HIGH.isFlagged()).isTrue();
		assertThat(RiskLevel.flaggedLevels()).containsExactlyInAnyOrder(RiskLevel.MEDIUM, RiskLevel.HIGH);
	}

}
