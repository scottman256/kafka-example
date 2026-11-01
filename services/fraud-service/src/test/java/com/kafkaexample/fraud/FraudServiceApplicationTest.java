package com.kafkaexample.fraud;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class FraudServiceApplicationTest {

	@Test
	void mainStartsTheSpringApplicationWithTheGivenArguments() {
		String[] arguments = { "--server.port=0" };
		try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
			FraudServiceApplication.main(arguments);

			springApplication.verify(() -> SpringApplication.run(FraudServiceApplication.class, arguments));
		}
	}

}
