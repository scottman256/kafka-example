import { describe, expect, it } from 'vitest';
import { SCENARIOS, scenarioDraft } from './scenarios';
import { validate } from './validation';

const TODAY = new Date(2026, 10, 1);

const scenarioNamed = (name: string) => SCENARIOS.find((scenario) => scenario.name === name)!;

describe('scenarioDraft', () => {
  it.each(SCENARIOS.map((scenario) => [scenario.name, scenario]))('builds a valid draft for %s', (_, scenario) => {
    expect(validate(scenarioDraft(scenario, TODAY))).toEqual({});
  });

  it('fills in the everyday purchase for anything a scenario leaves out', () => {
    expect(scenarioDraft(scenarioNamed('Everyday purchase'), TODAY)).toEqual({
      transactionDate: '2026-11-01',
      payer: 'Alice Smith',
      payee: "Bob's Hardware",
      amount: '125.50',
      memo: 'Lumber for deck',
      sourceBank: 'First National Bank',
    });
  });

  it('pays the same party for the self-payment', () => {
    const draft = scenarioDraft(scenarioNamed('Self-payment'), TODAY);

    expect(draft.payee).toBe(draft.payer);
  });

  it('dates the future-dated payment three weeks ahead', () => {
    expect(scenarioDraft(scenarioNamed('Future-dated'), TODAY).transactionDate).toBe('2026-11-22');
  });
});
