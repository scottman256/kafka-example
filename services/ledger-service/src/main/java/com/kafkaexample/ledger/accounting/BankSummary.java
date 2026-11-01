package com.kafkaexample.ledger.accounting;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** The funds that have left one source bank: the total paid from it and the number of payments. */
@Entity
@Table(name = "bank_summaries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BankSummary {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	private String bankName;

	private BigDecimal totalOutflow;

	private int transactionCount;

	public BankSummary(String bankName) {
		this.bankName = bankName;
		this.totalOutflow = BigDecimal.ZERO;
	}

	public void recordOutflow(BigDecimal amount) {
		totalOutflow = totalOutflow.add(amount);
		transactionCount++;
	}

}
