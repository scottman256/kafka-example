package com.kafkaexample.fraud.api;

import java.util.List;
import java.util.UUID;

import com.kafkaexample.fraud.screening.RiskAssessmentQueryService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/risk")
public class RiskAssessmentController {

	private final RiskAssessmentQueryService riskAssessmentQueryService;

	public RiskAssessmentController(RiskAssessmentQueryService riskAssessmentQueryService) {
		this.riskAssessmentQueryService = riskAssessmentQueryService;
	}

	@GetMapping("/assessments")
	public List<RiskAssessmentResponse> findRecent() {
		return riskAssessmentQueryService.findRecent().stream().map(RiskAssessmentResponse::from).toList();
	}

	@GetMapping("/assessments/{transactionId}")
	public RiskAssessmentResponse findByTransactionId(@PathVariable UUID transactionId) {
		return RiskAssessmentResponse.from(riskAssessmentQueryService.findByTransactionId(transactionId));
	}

	@GetMapping("/alerts")
	public List<RiskAssessmentResponse> findAlerts() {
		return riskAssessmentQueryService.findFlagged().stream().map(RiskAssessmentResponse::from).toList();
	}

}
