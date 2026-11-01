package com.kafkaexample.ledger.accounting;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One transaction in double-entry form. Its unique event id also makes posting idempotent.
 */
@Entity
@Table(name = "journal_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JournalEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	private UUID eventId;

	private UUID transactionId;

	private LocalDate transactionDate;

	private String payer;

	private String payee;

	private BigDecimal amount;

	private String sourceBank;

	private int kafkaPartition;

	private long kafkaOffset;

	private Instant postedAt;

	@OneToMany(mappedBy = "journalEntry", cascade = CascadeType.PERSIST)
	private List<JournalLine> lines = new ArrayList<>();

	public JournalEntry(TransactionCreatedEvent transaction, RecordPosition recordPosition, Instant postedAt) {
		this.eventId = transaction.eventId();
		this.transactionId = transaction.transactionId();
		this.transactionDate = transaction.transactionDate();
		this.payer = transaction.payer();
		this.payee = transaction.payee();
		this.amount = transaction.amount();
		this.sourceBank = transaction.sourceBank();
		this.kafkaPartition = recordPosition.partition();
		this.kafkaOffset = recordPosition.offset();
		this.postedAt = postedAt;
	}

	/**
	 * Debits the payer's account and credits the payee's with the same amount, so the entry's
	 * lines always sum to zero.
	 */
	public void post(Account payerAccount, Account payeeAccount) {
		addLine(payerAccount, amount.negate());
		addLine(payeeAccount, amount);
	}

	public List<JournalLine> getLines() {
		return List.copyOf(lines);
	}

	private void addLine(Account account, BigDecimal lineAmount) {
		lines.add(new JournalLine(this, account, lineAmount));
		account.post(lineAmount);
	}

}
