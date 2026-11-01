import { localDate, type TransactionDraft } from './draft';

export interface Scenario {
  name: string;
  expectation: string;
  fields: Partial<TransactionDraft>;
  daysFromToday?: number;
}

const EVERYDAY_PURCHASE: Omit<TransactionDraft, 'transactionDate'> = {
  payer: 'Alice Smith',
  payee: "Bob's Hardware",
  amount: '125.50',
  memo: 'Lumber for deck',
  sourceBank: 'First National Bank',
};

/** Cases that each trigger a different fraud-service rule. */
export const SCENARIOS: Scenario[] = [
  { name: 'Everyday purchase', expectation: 'Low risk', fields: {} },
  {
    name: 'Just under $10,000',
    expectation: 'Possible structuring',
    fields: {
      payer: 'Nina Patel',
      payee: 'Cash Exchange Co',
      amount: '9500.00',
      memo: 'Consulting',
      sourceBank: 'Harbor Savings Bank',
    },
  },
  {
    name: 'Large payment',
    expectation: 'Large amount',
    fields: {
      payer: 'Grace Hopper',
      payee: 'Acme Motors',
      amount: '15000.00',
      memo: 'New car',
      sourceBank: 'Summit Credit Union',
    },
  },
  {
    name: 'Self-payment',
    expectation: 'High risk',
    fields: {
      payer: 'Mallory Black',
      payee: 'Mallory Black',
      amount: '9500.00',
      memo: 'Moving money',
      sourceBank: 'Pioneer Trust',
    },
  },
  {
    name: 'Future-dated',
    expectation: 'Date anomaly',
    fields: {
      payer: 'Frank Ocean',
      payee: 'Landlord LLC',
      amount: '1200.00',
      memo: 'Rent',
      sourceBank: 'Evergreen Community Bank',
    },
    daysFromToday: 21,
  },
  {
    name: 'Velocity burst',
    expectation: 'Send it 4 times: the 4th is flagged',
    fields: {
      payer: 'Victor Quick',
      payee: 'Coffee Shop',
      amount: '4.50',
      memo: 'Coffee',
      sourceBank: 'Metro Federal Bank',
    },
  },
];

export function scenarioDraft(scenario: Scenario, today: Date): TransactionDraft {
  const transactionDate = new Date(today);
  transactionDate.setDate(today.getDate() + (scenario.daysFromToday ?? 0));
  return { ...EVERYDAY_PURCHASE, ...scenario.fields, transactionDate: localDate(transactionDate) };
}
