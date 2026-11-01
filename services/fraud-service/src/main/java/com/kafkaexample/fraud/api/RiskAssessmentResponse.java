package com.kafkaexample.fraud.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.kafkaexample.fraud.screening.RiskAssessment;
import com.kafkaexample.fraud.screening.RiskLevel;

public record RiskAssessmentResponse(
		UUID transactionId,
		UUID eventId,
		String payer,
		String payee,
		BigDecimal amount,
		int riskScore,
		RiskLevel riskLevel,
		boolean flagged,
		List<TriggeredRuleResponse> triggeredRules,
		KafkaPositionResponse kafka,
		Instant occurredAt,
		Instant assessedAt) {

	static RiskAssessmentResponse from(RiskAssessment assessment) {
		return new RiskAssessmentResponse(assessment.getTransactionId(), assessment.getEventId(), assessment.getPayer(),
				assessment.getPayee(), assessment.getAmount(), assessment.getRiskScore(), assessment.getRiskLevel(),
				assessment.getRiskLevel().isFlagged(), triggeredRules(assessment),
				new KafkaPositionResponse(assessment.getKafkaPartition(), assessment.getKafkaOffset()),
				assessment.getOccurredAt(), assessment.getAssessedAt());
	}

	private static List<TriggeredRuleResponse> triggeredRules(RiskAssessment assessment) {
		return assessment.getTriggeredRules().stream().sorted().map(TriggeredRuleResponse::from).toList();
	}

}
