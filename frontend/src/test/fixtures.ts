import type { LedgerSnapshot } from '../api/ledger';
import type { RiskAssessment } from '../api/risk';
import type { Transaction } from '../api/transactions';
import type { TransactionDraft } from '../form/draft';
import type { FakeRoutes } from './fakeApi';

export const SOURCE_BANKS = ['First National Bank', 'Summit Credit Union'];

export const transaction: Transaction = {
  id: '9b1d6c3e-4f7a-4c1e-8d2b-5a6f7e8d9c0b',
  transactionDate: '2026-11-01',
  payer: 'Alice Smith',
  payee: "Bob's Hardware",
  amount: 125.5,
  sourceBank: 'First National Bank',
  kafka: { status: 'PUBLISHED', topic: 'transactions.created', partition: 2, offset: 41, key: 'alice smith' },
};

export const assessment: RiskAssessment = {
  transactionId: transaction.id,
  payer: 'Mallory Black',
  payee: 'Mallory Black',
  amount: 9500,
  riskScore: 80,
  riskLevel: 'HIGH',
  triggeredRules: [
    { rule: 'POSSIBLE_STRUCTURING', points: 30, description: 'Amount is just under the $10,000 reporting threshold' },
    { rule: 'SELF_PAYMENT', points: 50, description: 'Payer and payee are the same party' },
  ],
};

export const ledger: LedgerSnapshot = {
  accounts: [
    { name: 'Alice Smith', balance: -125.5, entryCount: 1 },
    { name: "Bob's Hardware", balance: 125.5, entryCount: 1 },
  ],
  banks: [{ bankName: 'First National Bank', totalOutflow: 125.5, transactionCount: 1 }],
  trialBalance: { accountCount: 2, totalDebits: 125.5, totalCredits: 125.5, balanced: true },
};

export const validDraft: TransactionDraft = {
  transactionDate: '2026-11-01',
  payer: 'Alice Smith',
  payee: "Bob's Hardware",
  amount: '125.50',
  memo: 'Lumber for deck',
  sourceBank: 'First National Bank',
};

/** Every read endpoint the page polls, answering with the fixtures above. */
export function serviceRoutes(): FakeRoutes {
  return {
    'GET /api/transactions': { body: [transaction] },
    'GET /api/transactions/source-banks': { body: SOURCE_BANKS },
    'GET /api/risk/assessments': { body: [assessment] },
    'GET /api/ledger/accounts': { body: ledger.accounts },
    'GET /api/ledger/banks': { body: ledger.banks },
    'GET /api/ledger/trial-balance': { body: ledger.trialBalance },
  };
}
