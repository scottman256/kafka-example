import { expect, it } from 'vitest';
import { fakeApi } from '../test/fakeApi';
import { ledger, serviceRoutes } from '../test/fixtures';
import { fetchLedgerSnapshot } from './ledger';

it('fetchLedgerSnapshot combines the accounts, banks and trial balance', async () => {
  fakeApi(serviceRoutes());

  await expect(fetchLedgerSnapshot()).resolves.toEqual(ledger);
});
