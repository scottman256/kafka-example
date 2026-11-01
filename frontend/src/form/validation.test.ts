import { describe, expect, it } from 'vitest';
import { validDraft } from '../test/fixtures';
import { hasErrors, validate } from './validation';

describe('validate', () => {
  it('accepts a complete draft', () => {
    expect(validate(validDraft)).toEqual({});
  });

  it('reports every missing required field', () => {
    const blank = { transactionDate: '', payer: '', payee: '', amount: '', memo: '', sourceBank: '' };

    expect(Object.keys(validate(blank))).toEqual(['transactionDate', 'payer', 'payee', 'amount', 'sourceBank']);
  });

  it.each(['0.01', '125.5', ' 9500.00 ', '9999999999.99'])('accepts the amount %j', (amount) => {
    expect(validate({ ...validDraft, amount }).amount).toBeUndefined();
  });

  it.each(['0', '0.00', '-5', '10.001', '12345678901', '1e5', 'abc'])('rejects the amount %j', (amount) => {
    expect(validate({ ...validDraft, amount }).amount).toBeDefined();
  });

  it.each(['A', '  B  ', 'x'.repeat(101)])('rejects the party name %j', (payee) => {
    expect(validate({ ...validDraft, payee }).payee).toBe('Use between 2 and 100 characters.');
  });

  it('rejects a memo longer than 255 characters', () => {
    expect(validate({ ...validDraft, memo: 'x'.repeat(256) }).memo).toBeDefined();
  });
});

describe('hasErrors', () => {
  it('is true only when at least one field has an error', () => {
    expect(hasErrors({})).toBe(false);
    expect(hasErrors({ payer: 'Required' })).toBe(true);
  });
});
