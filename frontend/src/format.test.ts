import { describe, expect, it } from 'vitest';
import { formatMoney, humanize } from './format';

describe('formatMoney', () => {
  it('formats dollars with cents and thousands separators', () => {
    expect(formatMoney(9500)).toBe('$9,500.00');
    expect(formatMoney(-125.5)).toBe('-$125.50');
  });
});

describe('humanize', () => {
  it('turns a rule code into a sentence-case label', () => {
    expect(humanize('POSSIBLE_STRUCTURING')).toBe('Possible structuring');
    expect(humanize('NEW_PAYEE_HIGH_VALUE')).toBe('New payee high value');
  });
});
