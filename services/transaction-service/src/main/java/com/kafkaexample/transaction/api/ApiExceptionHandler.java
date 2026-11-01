package com.kafkaexample.transaction.api;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.kafkaexample.transaction.domain.TransactionNotFoundException;
import com.kafkaexample.transaction.domain.TransactionNotPublishedException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Extends Spring's handler so every error, including malformed JSON, is returned as an
 * RFC 9457 problem detail.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "One or more fields are invalid.");
		problem.setProperty("fieldErrors", fieldErrors(exception));
		return handleExceptionInternal(exception, problem, headers, status, request);
	}

	@ExceptionHandler(TransactionNotFoundException.class)
	public ProblemDetail handleNotFound(TransactionNotFoundException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	@ExceptionHandler(TransactionNotPublishedException.class)
	public ProblemDetail handleNotPublished(TransactionNotPublishedException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
				"The transaction was saved but could not be published to Kafka.");
		problem.setProperty("transactionId", exception.getTransactionId());
		return problem;
	}

	private static Map<String, String> fieldErrors(MethodArgumentNotValidException exception) {
		return exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage,
					(firstMessage, ignoredMessage) -> firstMessage, TreeMap::new));
	}

}
