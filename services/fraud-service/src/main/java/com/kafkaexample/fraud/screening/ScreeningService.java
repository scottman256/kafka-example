package com.kafkaexample.fraud.screening;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScreeningService {

	private static final Logger logger = LoggerFactory.getLogger(ScreeningService.class);

	private final RiskAssessmentRepository riskAssessmentRepository;

	private final RiskScorer riskScorer;

	private final Clock clock;

	ScreeningService(RiskAssessmentRepository riskAssessmentRepository, RiskScorer riskScorer, Clock clock) {
		this.riskAssessmentRepository = riskAssessmentRepository;
		this.riskScorer = riskScorer;
		this.clock = clock;
	}

	/**
	 * Idempotent: Kafka delivers at least once, so an event that has already been assessed is
	 * skipped rather than scored twice.
	 */
	@Transactional
	public void screen(TransactionCreatedEvent transaction, RecordPosition recordPosition) {
		if (riskAssessmentRepository.existsByEventId(transaction.eventId())) {
			logger.info("Skipping event {}: it has already been screened", transaction.eventId());
			return;
		}
		RiskScore riskScore = riskScorer.score(transaction);
		riskAssessmentRepository.save(RiskAssessment.of(transaction, riskScore, recordPosition, clock.instant()));
		logger.info("Screened transaction {}: score {} ({}) {}", transaction.transactionId(), riskScore.score(),
				riskScore.level(), riskScore.triggeredRules());
	}

}
