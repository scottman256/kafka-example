import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { expect, it } from 'vitest';
import { App } from './App';
import { fakeApi } from './test/fakeApi';
import { serviceRoutes, transaction } from './test/fixtures';

it('sends a scenario, shows where it was published, and fills every panel', async () => {
  fakeApi({ ...serviceRoutes(), 'POST /api/transactions': { status: 201, body: transaction } });
  const user = userEvent.setup();
  render(<App />);
  await screen.findByRole('option', { name: 'First National Bank' });

  await user.click(screen.getByRole('button', { name: 'Everyday purchase' }));
  expect(screen.getByLabelText('Payer')).toHaveValue('Alice Smith');
  await user.click(screen.getByRole('button', { name: 'Send transaction' }));

  const result = within(await screen.findByRole('region', { name: 'Published to Kafka' }));
  expect(result.getByText('41')).toBeInTheDocument();
  expect(within(screen.getByRole('region', { name: 'Transactions' })).getByText('$125.50')).toBeInTheDocument();
  expect(within(screen.getByRole('region', { name: 'Risk screening' })).getByText('HIGH 80')).toBeInTheDocument();
  expect(within(screen.getByRole('region', { name: 'Ledger' })).getByText('Balanced')).toBeInTheDocument();
});
