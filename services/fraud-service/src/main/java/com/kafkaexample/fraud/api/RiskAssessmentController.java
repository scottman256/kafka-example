package com.kafkaexample.fraud.api;

import java.util.List;
import java.util.UUID;

import com.kafkaexample.fraud.screening.RiskAssessment;
import com.kafkaexample.fraud.screening.RiskAssessmentQueryService;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
public class RiskAssessmentController {

	private final RiskAssessmentQueryService riskAssessmentQueryService;

	@GetMapping("/assessments")
	public List<RiskAssessmentResponse> findRecent() {
		return toResponses(riskAssessmentQueryService.findRecent());
	}

	@GetMapping("/assessments/{transactionId}")
	public RiskAssessmentResponse findByTransactionId(@PathVariable UUID transactionId) {
		return RiskAssessmentResponse.from(riskAssessmentQueryService.findByTransactionId(transactionId));
	}

	@GetMapping("/alerts")
	public List<RiskAssessmentResponse> findAlerts() {
		return toResponses(riskAssessmentQueryService.findFlagged());
	}

	private static List<RiskAssessmentResponse> toResponses(List<RiskAssessment> assessments) {
		return assessments.stream().map(RiskAssessmentResponse::from).toList();
	}

}
