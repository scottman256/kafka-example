package com.kafkaexample.fraud.screening;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RiskAssessmentQueryService {

	static final Limit RECENT_LIMIT = Limit.of(50);

	private final RiskAssessmentRepository riskAssessmentRepository;

	public List<RiskAssessment> findRecent() {
		return riskAssessmentRepository.findAllByOrderByAssessedAtDesc(RECENT_LIMIT);
	}

	public List<RiskAssessment> findFlagged() {
		return riskAssessmentRepository.findByRiskLevelInOrderByAssessedAtDesc(RiskLevel.flaggedLevels(), RECENT_LIMIT);
	}

	public RiskAssessment findByTransactionId(UUID transactionId) {
		return riskAssessmentRepository.findByTransactionId(transactionId)
			.orElseThrow(() -> new RiskAssessmentNotFoundException(transactionId));
	}

}
