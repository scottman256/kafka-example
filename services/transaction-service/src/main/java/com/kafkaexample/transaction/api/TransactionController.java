package com.kafkaexample.transaction.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.kafkaexample.transaction.domain.SourceBanks;
import com.kafkaexample.transaction.domain.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

	private final TransactionService transactionService;

	@PostMapping
	public ResponseEntity<TransactionResponse> create(@Valid @RequestBody CreateTransactionRequest request) {
		TransactionResponse response = TransactionResponse.from(transactionService.create(request.toNewTransaction()));
		return ResponseEntity.created(URI.create("/api/transactions/" + response.id())).body(response);
	}

	@GetMapping
	public List<TransactionResponse> findRecent() {
		return transactionService.findRecent().stream().map(TransactionResponse::from).toList();
	}

	@GetMapping("/{transactionId}")
	public TransactionResponse findById(@PathVariable UUID transactionId) {
		return TransactionResponse.from(transactionService.findById(transactionId));
	}

	@GetMapping("/source-banks")
	public List<String> sourceBanks() {
		return SourceBanks.ALL;
	}

}
