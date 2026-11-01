package com.kafkaexample.fraud.api;

import java.util.List;
import java.util.UUID;

import com.kafkaexample.fraud.screening.RiskAssessment;
import com.kafkaexample.fraud.screening.RiskAssessmentNotFoundException;
import com.kafkaexample.fraud.screening.RiskAssessmentQueryService;
import com.kafkaexample.fraud.screening.RiskRule;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static com.kafkaexample.fraud.ScreeningFixtures.RECORD_POSITION;
import static com.kafkaexample.fraud.ScreeningFixtures.assessment;
import static com.kafkaexample.fraud.ScreeningFixtures.payment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebMvcTest(RiskAssessmentController.class)
class RiskAssessmentControllerTest {

	@Autowired
	private MockMvcTester mockMvc;

	@MockitoBean
	private RiskAssessmentQueryService riskAssessmentQueryService;

	private final RiskAssessment selfPayment = assessment(payment("Alice Smith", "Alice Smith", "50.00"),
			RiskRule.SELF_PAYMENT);

	@Test
	void assessmentsListsTheRecentAssessments() {
		given(riskAssessmentQueryService.findRecent()).willReturn(List.of(selfPayment));

		assertThat(mockMvc.get().uri("/api/risk/assessments")).hasStatusOk()
			.bodyJson()
			.extractingPath("$[*].transactionId")
			.asArray()
			.containsExactly(selfPayment.getTransactionId().toString());
	}

	@Test
	void assessmentByTransactionIdShowsTheScoreRulesAndKafkaPosition() {
		given(riskAssessmentQueryService.findByTransactionId(selfPayment.getTransactionId())).willReturn(selfPayment);

		assertThat(mockMvc.get().uri("/api/risk/assessments/{id}", selfPayment.getTransactionId())).hasStatusOk()
			.bodyJson()
			.hasPathSatisfying("$.riskScore", score -> assertThat(score).isEqualTo(50))
			.hasPathSatisfying("$.riskLevel", level -> assertThat(level).asString().isEqualTo("MEDIUM"))
			.hasPathSatisfying("$.flagged", flagged -> assertThat(flagged).asBoolean().isTrue())
			.hasPathSatisfying("$.triggeredRules[0].rule", rule -> assertThat(rule).asString().isEqualTo("SELF_PAYMENT"))
			.hasPathSatisfying("$.triggeredRules[0].points", points -> assertThat(points).isEqualTo(50))
			.hasPathSatisfying("$.kafka.offset", offset -> assertThat(offset).asNumber().isEqualTo((int) RECORD_POSITION.offset()));
	}

	@Test
	void assessmentByTransactionIdIsNotFoundBeforeTheTransactionIsScreened() {
		UUID unscreenedTransactionId = UUID.randomUUID();
		given(riskAssessmentQueryService.findByTransactionId(unscreenedTransactionId))
			.willThrow(new RiskAssessmentNotFoundException(unscreenedTransactionId));

		assertThat(mockMvc.get().uri("/api/risk/assessments/{id}", unscreenedTransactionId))
			.hasStatus(HttpStatus.NOT_FOUND)
			.bodyJson()
			.extractingPath("$.detail")
			.asString()
			.contains(unscreenedTransactionId.toString());
	}

	@Test
	void assessmentByTransactionIdRejectsAnIdThatIsNotAUuid() {
		assertThat(mockMvc.get().uri("/api/risk/assessments/not-a-uuid")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void alertsListsTheFlaggedAssessments() {
		given(riskAssessmentQueryService.findFlagged()).willReturn(List.of(selfPayment));

		assertThat(mockMvc.get().uri("/api/risk/alerts")).hasStatusOk()
			.bodyJson()
			.extractingPath("$[*].flagged")
			.asArray()
			.containsExactly(true);
	}

}
