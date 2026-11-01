import { useState, type FormEvent } from 'react';
import type { TransactionDraft } from './draft';
import { submitTransaction, type ResultOutcome } from './submit';
import { hasErrors, validate, type FieldErrors } from './validation';

export interface TransactionSubmission {
  errors: FieldErrors;
  submitting: boolean;
  handleSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>;
}

/** Validates the draft, sends it, and puts any field errors from the server back on the form. */
export function useTransactionSubmission(
  draft: TransactionDraft,
  onOutcome: (outcome: ResultOutcome) => void,
): TransactionSubmission {
  const [errors, setErrors] = useState<FieldErrors>({});
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const validationErrors = validate(draft);
    setErrors(validationErrors);
    if (hasErrors(validationErrors)) return;
    setSubmitting(true);
    const outcome = await submitTransaction(draft);
    setSubmitting(false);
    if (outcome.status === 'rejected') setErrors(outcome.fieldErrors);
    else onOutcome(outcome);
  }

  return { errors, submitting, handleSubmit };
}
