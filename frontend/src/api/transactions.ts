import { getJson, postJson } from './http';

export interface KafkaPublication {
  status: 'PENDING' | 'PUBLISHED' | 'FAILED';
  topic: string;
  partition: number | null;
  offset: number | null;
  key: string;
}

export interface Transaction {
  id: string;
  transactionDate: string;
  payer: string;
  payee: string;
  amount: number;
  sourceBank: string;
  kafka: KafkaPublication | null;
}

export interface NewTransaction {
  transactionDate: string;
  payer: string;
  payee: string;
  amount: number;
  memo: string;
  sourceBank: string;
}

const TRANSACTIONS_URL = '/api/transactions';

export const createTransaction = (transaction: NewTransaction) =>
  postJson<Transaction>(TRANSACTIONS_URL, transaction);

export const fetchRecentTransactions = () => getJson<Transaction[]>(TRANSACTIONS_URL);

export const fetchSourceBanks = () => getJson<string[]>(`${TRANSACTIONS_URL}/source-banks`);
