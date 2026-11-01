package com.kafkaexample.transaction.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = KnownSourceBankValidator.class)
public @interface KnownSourceBank {

	String message() default "must be one of the supported source banks";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
