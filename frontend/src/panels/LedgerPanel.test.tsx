import { render, screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { fakeApi } from '../test/fakeApi';
import { ledger, serviceRoutes } from '../test/fixtures';
import { LedgerPanel } from './LedgerPanel';

describe('LedgerPanel', () => {
  it('shows the trial balance, every account balance and each bank outflow', async () => {
    fakeApi(serviceRoutes());
    render(<LedgerPanel />);

    expect(await screen.findByText(/debits \$125\.50, credits \$125\.50 across 2 accounts/)).toHaveClass(
      'trial-balance--balanced',
    );
    const accounts = within(screen.getByRole('table', { name: 'Account balances' }));
    expect(accounts.getByRole('cell', { name: '-$125.50' })).toHaveClass('amount--negative');
    expect(accounts.getByRole('cell', { name: '$125.50' })).toHaveClass('amount--positive');
    const banks = within(screen.getByRole('table', { name: 'Outflow by source bank' }));
    expect(banks.getByRole('row', { name: 'First National Bank $125.50 1' })).toBeInTheDocument();
  });

  it('flags a ledger that is out of balance', async () => {
    const trialBalance = { ...ledger.trialBalance, totalCredits: 100, balanced: false };
    fakeApi({ ...serviceRoutes(), 'GET /api/ledger/trial-balance': { body: trialBalance } });
    render(<LedgerPanel />);

    expect(await screen.findByText('Out of balance')).toBeInTheDocument();
    expect(screen.getByText(/across 2 accounts/)).toHaveClass('trial-balance--unbalanced');
  });

  it('says when nothing has been posted yet', async () => {
    fakeApi({ ...serviceRoutes(), 'GET /api/ledger/accounts': { body: [] } });
    render(<LedgerPanel />);

    expect(await screen.findByText('Nothing posted yet.')).toBeInTheDocument();
  });
});
