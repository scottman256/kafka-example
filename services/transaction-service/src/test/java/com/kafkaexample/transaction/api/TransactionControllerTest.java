package com.kafkaexample.transaction.api;

import java.util.List;

import com.kafkaexample.transaction.domain.NewTransaction;
import com.kafkaexample.transaction.domain.Transaction;
import com.kafkaexample.transaction.domain.TransactionNotFoundException;
import com.kafkaexample.transaction.domain.TransactionNotPublishedException;
import com.kafkaexample.transaction.domain.TransactionService;
import com.kafkaexample.transaction.domain.TransactionWithPublishLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static com.kafkaexample.transaction.TransactionFixtures.CREATE_REQUEST_JSON;
import static com.kafkaexample.transaction.TransactionFixtures.OFFSET;
import static com.kafkaexample.transaction.TransactionFixtures.PARTITION;
import static com.kafkaexample.transaction.TransactionFixtures.TOPIC;
import static com.kafkaexample.transaction.TransactionFixtures.TRANSACTION_ID;
import static com.kafkaexample.transaction.TransactionFixtures.publishedLogEntry;
import static com.kafkaexample.transaction.TransactionFixtures.savedTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

	@Autowired
	private MockMvcTester mockMvc;

	@MockitoBean
	private TransactionService transactionService;

	private final Transaction transaction = savedTransaction();

	@Test
	void createReturnsTheTransactionWithItsKafkaPublication() {
		givenTheServicePublishesTheTransaction();

		assertThat(post(CREATE_REQUEST_JSON)).hasStatus(HttpStatus.CREATED)
			.hasHeader("Location", "/api/transactions/" + TRANSACTION_ID)
			.bodyJson()
			.hasPathSatisfying("$.id", id -> assertThat(id).asString().isEqualTo(TRANSACTION_ID.toString()))
			.hasPathSatisfying("$.kafka.status", status -> assertThat(status).asString().isEqualTo("PUBLISHED"))
			.hasPathSatisfying("$.kafka.topic", topic -> assertThat(topic).asString().isEqualTo(TOPIC))
			.hasPathSatisfying("$.kafka.partition", partition -> assertThat(partition).isEqualTo(PARTITION))
			.hasPathSatisfying("$.kafka.offset", offset -> assertThat(offset).asNumber().isEqualTo((int) OFFSET));
	}

	@Test
	void createPassesTheStrippedRequestToTheService() {
		givenTheServicePublishesTheTransaction();

		post(CREATE_REQUEST_JSON);

		ArgumentCaptor<NewTransaction> newTransactionCaptor = ArgumentCaptor.forClass(NewTransaction.class);
		verify(transactionService).create(newTransactionCaptor.capture());
		assertThat(newTransactionCaptor.getValue().payer()).isEqualTo("Alice Smith");
	}

	@Test
	void createReportsEveryMissingRequiredField() {
		assertThat(post("{}")).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.fieldErrors")
			.asMap()
			.containsOnlyKeys("transactionDate", "payer", "payee", "amount", "sourceBank");
		verifyNoInteractions(transactionService);
	}

	@Test
	void createRejectsAnUnknownSourceBank() {
		assertThat(post(CREATE_REQUEST_JSON.replace("First National Bank", "Bank of Nowhere")))
			.hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.fieldErrors.sourceBank")
			.isEqualTo("must be one of the supported source banks");
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "-5.00", "10.001" })
	void createRejectsAmountsThatAreNotPositiveWholeCents(String amount) {
		assertThat(post(CREATE_REQUEST_JSON.replace("125.50", amount))).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.hasPath("$.fieldErrors.amount");
	}

	@Test
	void createRejectsAPayerShorterThanTwoCharactersOnceStripped() {
		assertThat(post(CREATE_REQUEST_JSON.replace("  Alice Smith ", " A "))).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.hasPath("$.fieldErrors.payer");
	}

	@Test
	void createRejectsMalformedJson() {
		assertThat(post("{ not json")).hasStatus(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(transactionService);
	}

	@Test
	void createReturnsServiceUnavailableWithTheTransactionIdWhenPublishingFails() {
		given(transactionService.create(any(NewTransaction.class)))
			.willThrow(new TransactionNotPublishedException(TRANSACTION_ID, "broker unavailable"));

		assertThat(post(CREATE_REQUEST_JSON)).hasStatus(HttpStatus.SERVICE_UNAVAILABLE)
			.bodyJson()
			.extractingPath("$.transactionId")
			.isEqualTo(TRANSACTION_ID.toString());
	}

	@Test
	void findRecentListsTheRecentTransactions() {
		given(transactionService.findRecent()).willReturn(List.of(new TransactionWithPublishLog(transaction, null)));

		assertThat(mockMvc.get().uri("/api/transactions")).hasStatusOk()
			.bodyJson()
			.extractingPath("$[*].id")
			.asArray()
			.containsExactly(TRANSACTION_ID.toString());
	}

	@Test
	void findByIdReturnsNotFoundForAnUnknownTransaction() {
		given(transactionService.findById(TRANSACTION_ID)).willThrow(new TransactionNotFoundException(TRANSACTION_ID));

		assertThat(mockMvc.get().uri("/api/transactions/{id}", TRANSACTION_ID)).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void sourceBanksListsTheSupportedBanks() {
		assertThat(mockMvc.get().uri("/api/transactions/source-banks")).hasStatusOk()
			.bodyJson()
			.extractingPath("$")
			.asArray()
			.contains("First National Bank", "Summit Credit Union");
	}

	private void givenTheServicePublishesTheTransaction() {
		given(transactionService.create(any(NewTransaction.class)))
			.willReturn(new TransactionWithPublishLog(transaction, publishedLogEntry(transaction)));
	}

	private MvcTestResult post(String json) {
		return mockMvc.post().uri("/api/transactions").contentType(MediaType.APPLICATION_JSON).content(json).exchange();
	}

}
