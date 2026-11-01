import type { TransactionDraft } from './draft';

export type FieldErrors = Partial<Record<keyof TransactionDraft, string>>;

const AMOUNT_FORMAT = /^\d{1,10}(\.\d{1,2})?$/;

/** The same rules transaction-service applies, so most mistakes are caught before a request. */
const RULES: Record<keyof TransactionDraft, (value: string) => string | undefined> = {
  transactionDate: (value) => (value ? undefined : 'Choose the transaction date.'),
  payer: partyNameError,
  payee: partyNameError,
  amount: (value) => (isValidAmount(value.trim()) ? undefined : 'Enter at least 0.01, with up to 2 decimals.'),
  memo: (value) => (value.trim().length <= 255 ? undefined : 'Keep the memo to 255 characters or fewer.'),
  sourceBank: (value) => (value ? undefined : 'Choose the bank the money comes from.'),
};

export function validate(draft: TransactionDraft): FieldErrors {
  const fields = Object.keys(RULES) as (keyof TransactionDraft)[];
  return Object.fromEntries(
    fields.map((field) => [field, RULES[field](draft[field])]).filter(([, error]) => error !== undefined),
  );
}

export function hasErrors(errors: FieldErrors): boolean {
  return Object.keys(errors).length > 0;
}

function partyNameError(value: string): string | undefined {
  const length = value.trim().length;
  return length >= 2 && length <= 100 ? undefined : 'Use between 2 and 100 characters.';
}

function isValidAmount(value: string): boolean {
  return AMOUNT_FORMAT.test(value) && Number(value) >= 0.01;
}
