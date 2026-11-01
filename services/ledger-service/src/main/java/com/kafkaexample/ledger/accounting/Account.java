package com.kafkaexample.ledger.accounting;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A party's account, opened the first time the party appears. The balance is the party's net
 * position: everything it has been paid minus everything it has paid.
 */
@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	private String name;

	private String normalizedName;

	private BigDecimal balance;

	private int entryCount;

	private Instant openedAt;

	public Account(String name, Instant openedAt) {
		this.name = name;
		this.normalizedName = normalizeName(name);
		this.balance = BigDecimal.ZERO;
		this.openedAt = openedAt;
	}

	public static String normalizeName(String name) {
		return name.strip().toLowerCase(Locale.ROOT);
	}

	void post(BigDecimal amount) {
		balance = balance.add(amount);
		entryCount++;
	}

}
