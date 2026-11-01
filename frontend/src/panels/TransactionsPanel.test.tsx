import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import type { KafkaPublication } from '../api/transactions';
import { fakeApi, unreachableApi } from '../test/fakeApi';
import { serviceRoutes, transaction } from '../test/fixtures';
import { TransactionsPanel } from './TransactionsPanel';

describe('TransactionsPanel', () => {
  it('lists recent transactions and where each landed in Kafka', async () => {
    fakeApi(serviceRoutes());
    render(<TransactionsPanel />);

    expect(await screen.findByText("Alice Smith → Bob's Hardware")).toBeInTheDocument();
    expect(screen.getByText('$125.50')).toBeInTheDocument();
    expect(screen.getByText('2026-11-01 · First National Bank · partition 2, offset 41')).toBeInTheDocument();
  });

  it.each<[string, KafkaPublication | null]>([
    ['has no publish log entry', null],
    ['failed to publish', { ...transaction.kafka!, status: 'FAILED', partition: null, offset: null }],
  ])('flags a transaction that %s', async (_, kafka) => {
    fakeApi({ 'GET /api/transactions': { body: [{ ...transaction, kafka }] } });
    render(<TransactionsPanel />);

    expect(await screen.findByText(/not published to Kafka/)).toBeInTheDocument();
  });

  it('says when there are no transactions yet', async () => {
    fakeApi({ 'GET /api/transactions': { body: [] } });
    render(<TransactionsPanel />);

    expect(await screen.findByText('No transactions yet.')).toBeInTheDocument();
  });

  it('warns when transaction-service cannot be reached', async () => {
    unreachableApi();
    render(<TransactionsPanel />);

    expect(await screen.findByRole('alert')).toHaveTextContent("Can't reach transaction-service right now.");
  });
});
