import { describe, expect, it } from 'vitest';
import { validDraft } from '../test/fixtures';
import { emptyDraft, localDate, toNewTransaction } from './draft';

describe('localDate', () => {
  it('uses the local calendar date, even late in the evening', () => {
    expect(localDate(new Date(2026, 10, 1, 23, 30))).toBe('2026-11-01');
  });
});

describe('emptyDraft', () => {
  it('starts on today with every other field blank', () => {
    expect(emptyDraft(new Date(2026, 10, 1))).toEqual({
      transactionDate: '2026-11-01',
      payer: '',
      payee: '',
      amount: '',
      memo: '',
      sourceBank: '',
    });
  });
});

describe('toNewTransaction', () => {
  it('sends the amount as a number and the other fields as typed', () => {
    expect(toNewTransaction(validDraft)).toEqual({ ...validDraft, amount: 125.5 });
  });
});
