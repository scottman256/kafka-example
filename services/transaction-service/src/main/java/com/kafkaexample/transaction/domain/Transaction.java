package com.kafkaexample.transaction.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.kafkaexample.transaction.publishing.TransactionCreatedEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transactions")
public class Transaction {

	public static final String CURRENCY = "USD";

	private static final int AMOUNT_SCALE = 2;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	private LocalDate transactionDate;

	private String payer;

	private String payee;

	private BigDecimal amount;

	private String currency;

	private String memo;

	private String sourceBank;

	private Instant createdAt;

	protected Transaction() {
	}

	private Transaction(NewTransaction newTransaction, Instant createdAt) {
		this.transactionDate = newTransaction.transactionDate();
		this.payer = newTransaction.payer();
		this.payee = newTransaction.payee();
		this.amount = newTransaction.amount().setScale(AMOUNT_SCALE);
		this.currency = CURRENCY;
		this.memo = newTransaction.memo();
		this.sourceBank = newTransaction.sourceBank();
		this.createdAt = createdAt;
	}

	public static Transaction create(NewTransaction newTransaction, Instant createdAt) {
		return new Transaction(newTransaction, createdAt);
	}

	public TransactionCreatedEvent toCreatedEvent(Instant occurredAt) {
		return new TransactionCreatedEvent(UUID.randomUUID(), TransactionCreatedEvent.EVENT_TYPE,
				TransactionCreatedEvent.EVENT_VERSION, occurredAt, id, transactionDate, payer, payee, amount,
				currency, memo, sourceBank);
	}

	public UUID getId() {
		return id;
	}

	public LocalDate getTransactionDate() {
		return transactionDate;
	}

	public String getPayer() {
		return payer;
	}

	public String getPayee() {
		return payee;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getCurrency() {
		return currency;
	}

	public String getMemo() {
		return memo;
	}

	public String getSourceBank() {
		return sourceBank;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
