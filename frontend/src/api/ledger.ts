import { getJson } from './http';

export interface Account {
  name: string;
  balance: number;
  entryCount: number;
}

export interface BankSummary {
  bankName: string;
  totalOutflow: number;
  transactionCount: number;
}

export interface TrialBalance {
  accountCount: number;
  totalDebits: number;
  totalCredits: number;
  balanced: boolean;
}

export interface LedgerSnapshot {
  accounts: Account[];
  banks: BankSummary[];
  trialBalance: TrialBalance;
}

export async function fetchLedgerSnapshot(): Promise<LedgerSnapshot> {
  const [accounts, banks, trialBalance] = await Promise.all([
    getJson<Account[]>('/api/ledger/accounts'),
    getJson<BankSummary[]>('/api/ledger/banks'),
    getJson<TrialBalance>('/api/ledger/trial-balance'),
  ]);
  return { accounts, banks, trialBalance };
}
