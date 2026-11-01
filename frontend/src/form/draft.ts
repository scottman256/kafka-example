import type { NewTransaction } from '../api/transactions';

/** What the form holds while the user types: every field as text. */
export interface TransactionDraft {
  transactionDate: string;
  payer: string;
  payee: string;
  amount: string;
  memo: string;
  sourceBank: string;
}

/** The date in the user's own time zone, as yyyy-mm-dd. */
export function localDate(date: Date): string {
  const localTime = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return localTime.toISOString().slice(0, 10);
}

export function emptyDraft(today: Date): TransactionDraft {
  return { transactionDate: localDate(today), payer: '', payee: '', amount: '', memo: '', sourceBank: '' };
}

export function toNewTransaction(draft: TransactionDraft): NewTransaction {
  return { ...draft, amount: Number(draft.amount) };
}
