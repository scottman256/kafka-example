import { describe, expect, it } from 'vitest';
import { fakeApi, unreachableApi } from '../test/fakeApi';
import { transaction, validDraft } from '../test/fixtures';
import { submitTransaction } from './submit';

describe('submitTransaction', () => {
  it('is published when transaction-service accepts it', async () => {
    fakeApi({ 'POST /api/transactions': { status: 201, body: transaction } });

    await expect(submitTransaction(validDraft)).resolves.toEqual({ status: 'published', transaction });
  });

  it('is rejected with the field errors from a validation failure', async () => {
    fakeApi({ 'POST /api/transactions': { status: 400, body: { fieldErrors: { payer: 'must not be blank' } } } });

    await expect(submitTransaction(validDraft)).resolves.toEqual({
      status: 'rejected',
      fieldErrors: { payer: 'must not be blank' },
    });
  });

  it('fails with the service explanation when the transaction is saved but not published', async () => {
    const detail = 'The transaction was saved but could not be published to Kafka.';
    fakeApi({ 'POST /api/transactions': { status: 503, body: { detail } } });

    await expect(submitTransaction(validDraft)).resolves.toEqual({ status: 'failed', message: detail });
  });

  it('fails clearly when transaction-service cannot be reached', async () => {
    unreachableApi();

    await expect(submitTransaction(validDraft)).resolves.toEqual({
      status: 'failed',
      message: 'Could not reach transaction-service.',
    });
  });
});
