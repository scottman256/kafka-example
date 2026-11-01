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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

	public Transaction(NewTransaction newTransaction, Instant createdAt) {
		this.transactionDate = newTransaction.transactionDate();
		this.payer = newTransaction.payer();
		this.payee = newTransaction.payee();
		this.amount = newTransaction.amount().setScale(AMOUNT_SCALE);
		this.currency = CURRENCY;
		this.memo = newTransaction.memo();
		this.sourceBank = newTransaction.sourceBank();
		this.createdAt = createdAt;
	}

	public TransactionCreatedEvent toCreatedEvent(Instant occurredAt) {
		return new TransactionCreatedEvent(UUID.randomUUID(), TransactionCreatedEvent.EVENT_TYPE,
				TransactionCreatedEvent.EVENT_VERSION, occurredAt, id, transactionDate, payer, payee, amount,
				currency, memo, sourceBank);
	}

}
