package com.kafkaexample.transaction;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class TransactionServiceApplicationTest {

	@Test
	void mainStartsTheSpringApplicationWithTheGivenArguments() {
		String[] arguments = { "--server.port=0" };
		try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
			TransactionServiceApplication.main(arguments);

			springApplication.verify(() -> SpringApplication.run(TransactionServiceApplication.class, arguments));
		}
	}

}
