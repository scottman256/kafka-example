package com.kafkaexample.fraud.screening;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The screening result for one transaction, together with the transaction details the
 * velocity and new-payee checks look up. Its unique event id also makes screening idempotent.
 */
@Entity
@Table(name = "risk_assessments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RiskAssessment {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	private UUID eventId;

	private UUID transactionId;

	private String payer;

	private String payee;

	private String normalizedPayer;

	private String normalizedPayee;

	private BigDecimal amount;

	private Instant occurredAt;

	private int riskScore;

	@Enumerated(EnumType.STRING)
	private RiskLevel riskLevel;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "triggered_rules", joinColumns = @JoinColumn(name = "risk_assessment_id"))
	@Column(name = "rule_code")
	@Enumerated(EnumType.STRING)
	private Set<RiskRule> triggeredRules = new HashSet<>();

	private int kafkaPartition;

	private long kafkaOffset;

	private Instant assessedAt;

	public RiskAssessment(TransactionCreatedEvent transaction, RiskScore riskScore, RecordPosition recordPosition,
			Instant assessedAt) {
		this.eventId = transaction.eventId();
		this.transactionId = transaction.transactionId();
		this.payer = transaction.payer();
		this.payee = transaction.payee();
		this.normalizedPayer = transaction.normalizedPayer();
		this.normalizedPayee = transaction.normalizedPayee();
		this.amount = transaction.amount();
		this.occurredAt = transaction.occurredAt();
		this.riskScore = riskScore.score();
		this.riskLevel = riskScore.level();
		this.triggeredRules = new HashSet<>(riskScore.triggeredRules());
		this.kafkaPartition = recordPosition.partition();
		this.kafkaOffset = recordPosition.offset();
		this.assessedAt = assessedAt;
	}

	public Set<RiskRule> getTriggeredRules() {
		return Set.copyOf(triggeredRules);
	}

}
