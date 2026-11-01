package com.kafkaexample.ledger.api;

import java.math.BigDecimal;

import com.kafkaexample.ledger.accounting.TrialBalance;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrialBalanceResponseTest {

	@Test
	void fromIncludesTheNetBalanceAndWhetherItBalances() {
		TrialBalanceResponse response = TrialBalanceResponse
			.from(new TrialBalance(3, new BigDecimal("150.00"), new BigDecimal("150.00")));

		assertThat(response).isEqualTo(new TrialBalanceResponse(3, new BigDecimal("150.00"), new BigDecimal("150.00"),
				new BigDecimal("0.00"), true));
	}

}
