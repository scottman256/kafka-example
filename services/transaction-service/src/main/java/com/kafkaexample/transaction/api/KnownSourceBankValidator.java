package com.kafkaexample.transaction.api;

import com.kafkaexample.transaction.domain.SourceBanks;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class KnownSourceBankValidator implements ConstraintValidator<KnownSourceBank, String> {

	/** Null is left to {@code @NotBlank}, so a missing bank reports one error, not two. */
	@Override
	public boolean isValid(String sourceBank, ConstraintValidatorContext context) {
		return sourceBank == null || SourceBanks.isKnown(sourceBank);
	}

}
