import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { fakeApi, unreachableApi } from '../test/fakeApi';
import { SOURCE_BANKS, transaction, validDraft } from '../test/fixtures';
import { emptyDraft, type TransactionDraft } from './draft';
import { TransactionForm } from './TransactionForm';

const BANKS_ROUTE = { 'GET /api/transactions/source-banks': { body: SOURCE_BANKS } };

function renderForm(draft: TransactionDraft) {
  const onOutcome = vi.fn();
  const onDraftChange = vi.fn();
  render(<TransactionForm draft={draft} onDraftChange={onDraftChange} onOutcome={onOutcome} />);
  return { onOutcome, onDraftChange };
}

const sendButton = () => screen.getByRole('button', { name: 'Send transaction' });

describe('TransactionForm', () => {
  it('offers the banks transaction-service accepts', async () => {
    fakeApi(BANKS_ROUTE);
    renderForm(validDraft);

    expect(await screen.findByRole('option', { name: 'Summit Credit Union' })).toBeInTheDocument();
  });

  it('says so when the banks cannot be loaded', async () => {
    unreachableApi();
    renderForm(validDraft);

    expect(await screen.findByText('Could not load the banks from transaction-service.')).toBeInTheDocument();
  });

  it('reports each edit as an updated draft', async () => {
    fakeApi(BANKS_ROUTE);
    const { onDraftChange } = renderForm(emptyDraft(new Date(2026, 10, 1)));

    await userEvent.type(screen.getByLabelText('Payer'), 'A');

    expect(onDraftChange).toHaveBeenCalledWith(expect.objectContaining({ payer: 'A' }));
  });

  it('shows client-side errors without sending anything', async () => {
    const fetch = fakeApi(BANKS_ROUTE);
    renderForm({ ...validDraft, payer: 'A', amount: '0' });

    await userEvent.click(sendButton());

    expect(screen.getByLabelText('Payer')).toHaveAccessibleDescription('Use between 2 and 100 characters.');
    expect(screen.getByLabelText('Amount (USD)')).toHaveAttribute('aria-invalid', 'true');
    expect(fetch).not.toHaveBeenCalledWith('/api/transactions', expect.objectContaining({ method: 'POST' }));
  });

  it('passes a published transaction on', async () => {
    fakeApi({ ...BANKS_ROUTE, 'POST /api/transactions': { status: 201, body: transaction } });
    const { onOutcome } = renderForm(validDraft);

    await userEvent.click(sendButton());

    await waitFor(() => expect(onOutcome).toHaveBeenCalledWith({ status: 'published', transaction }));
  });

  it('shows the field errors the server sends back', async () => {
    const fieldErrors = { sourceBank: 'must be one of the supported source banks' };
    fakeApi({ ...BANKS_ROUTE, 'POST /api/transactions': { status: 400, body: { fieldErrors } } });
    const { onOutcome } = renderForm(validDraft);

    await userEvent.click(sendButton());

    expect(await screen.findByText('must be one of the supported source banks')).toBeInTheDocument();
    expect(onOutcome).not.toHaveBeenCalled();
  });

  it('disables the button while the transaction is being sent', async () => {
    let respond: (response: Response) => void = () => {};
    vi.stubGlobal('fetch', vi.fn((_: string, init?: RequestInit) =>
      init?.method === 'POST' ? new Promise((resolve) => (respond = resolve)) : Response.json(SOURCE_BANKS)));
    const { onOutcome } = renderForm(validDraft);

    await userEvent.click(sendButton());

    expect(screen.getByRole('button', { name: 'Sending…' })).toBeDisabled();
    respond(Response.json(transaction, { status: 201 }));
    await waitFor(() => expect(sendButton()).toBeEnabled());
    expect(onOutcome).toHaveBeenCalled();
  });
});
