import { ApiError } from '../api/http';
import { createTransaction, type Transaction } from '../api/transactions';
import { toNewTransaction, type TransactionDraft } from './draft';
import type { FieldErrors } from './validation';

export type SubmitOutcome =
  | { status: 'published'; transaction: Transaction }
  | { status: 'rejected'; fieldErrors: FieldErrors }
  | { status: 'failed'; message: string };

/** The outcomes shown in the result card. Rejected fields are shown on the form instead. */
export type ResultOutcome = Exclude<SubmitOutcome, { status: 'rejected' }>;

export async function submitTransaction(draft: TransactionDraft): Promise<SubmitOutcome> {
  try {
    return { status: 'published', transaction: await createTransaction(toNewTransaction(draft)) };
  } catch (error) {
    return failedOutcome(error);
  }
}

function failedOutcome(error: unknown): SubmitOutcome {
  if (!(error instanceof ApiError)) {
    return { status: 'failed', message: 'Could not reach transaction-service.' };
  }
  const { fieldErrors } = error.problem;
  return fieldErrors ? { status: 'rejected', fieldErrors } : { status: 'failed', message: error.message };
}
