import { render, screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { transaction } from '../test/fixtures';
import { SubmitResult } from './SubmitResult';

describe('SubmitResult', () => {
  it('invites a first transaction before anything has been sent', () => {
    render(<SubmitResult />);

    expect(screen.getByText('Send a transaction to see where it landed in Kafka.')).toBeInTheDocument();
  });

  it('shows where a published transaction landed in Kafka', () => {
    render(<SubmitResult outcome={{ status: 'published', transaction }} />);

    const result = within(screen.getByRole('region', { name: 'Published to Kafka' }));
    expect(result.getByText(transaction.id)).toBeInTheDocument();
    expect(result.getByText('transactions.created')).toBeInTheDocument();
    expect(result.getByText('2')).toBeInTheDocument();
    expect(result.getByText('41')).toBeInTheDocument();
    expect(result.getByText('alice smith')).toBeInTheDocument();
  });

  it('explains why a transaction could not be sent', () => {
    render(<SubmitResult outcome={{ status: 'failed', message: 'Could not reach transaction-service.' }} />);

    expect(screen.getByRole('alert')).toHaveTextContent('Could not reach transaction-service.');
  });
});
