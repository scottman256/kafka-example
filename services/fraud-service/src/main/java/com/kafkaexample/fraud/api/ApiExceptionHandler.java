package com.kafkaexample.fraud.api;

import com.kafkaexample.fraud.screening.RiskAssessmentNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Extends Spring's handler so every error is returned as an RFC 9457 problem detail. */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(RiskAssessmentNotFoundException.class)
	public ProblemDetail handleNotFound(RiskAssessmentNotFoundException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
	}

}
