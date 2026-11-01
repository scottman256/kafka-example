package com.kafkaexample.ledger;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class LedgerServiceApplicationTest {

	@Test
	void mainStartsTheSpringApplicationWithTheGivenArguments() {
		String[] arguments = { "--server.port=0" };
		try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
			LedgerServiceApplication.main(arguments);

			springApplication.verify(() -> SpringApplication.run(LedgerServiceApplication.class, arguments));
		}
	}

}
