package com.kafkaexample.fraud.screening;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID> {

	boolean existsByEventId(UUID eventId);

	boolean existsByNormalizedPayerAndNormalizedPayee(String normalizedPayer, String normalizedPayee);

	Optional<RiskAssessment> findByTransactionId(UUID transactionId);

	List<RiskAssessment> findAllByOrderByAssessedAtDesc(Limit limit);

	List<RiskAssessment> findByRiskLevelInOrderByAssessedAtDesc(Set<RiskLevel> riskLevels, Limit limit);

	@Query("""
			select count(assessment) from RiskAssessment assessment
			where assessment.normalizedPayer = :normalizedPayer
			  and assessment.occurredAt >= :from
			  and assessment.occurredAt < :until
			""")
	long countPaymentsByPayerBetween(String normalizedPayer, Instant from, Instant until);

}
