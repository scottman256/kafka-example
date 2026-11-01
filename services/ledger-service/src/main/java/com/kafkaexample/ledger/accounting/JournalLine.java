package com.kafkaexample.ledger.accounting;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "journal_lines")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JournalLine {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "journal_entry_id")
	private JournalEntry journalEntry;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id")
	private Account account;

	/** Negative for a debit (money out of the account), positive for a credit (money in). */
	private BigDecimal amount;

	JournalLine(JournalEntry journalEntry, Account account, BigDecimal amount) {
		this.journalEntry = journalEntry;
		this.account = account;
		this.amount = amount;
	}

	public EntrySide getSide() {
		return (amount.signum() < 0) ? EntrySide.DEBIT : EntrySide.CREDIT;
	}

	/** The other party: the payee for the payer's debit, and the payer for the payee's credit. */
	public String getCounterparty() {
		return (getSide() == EntrySide.DEBIT) ? journalEntry.getPayee() : journalEntry.getPayer();
	}

}
