import { randomUUID } from 'node:crypto';
import { expect, test } from '@playwright/test';
import { PaymentsDemoPage } from '../pages/PaymentsDemoPage';

/** Unique to this run, so each test finds its own rows even on a stack that already has data. */
const uniqueName = (prefix: string) => `${prefix} ${randomUUID().slice(0, 8)}`;

let demo: PaymentsDemoPage;

test.beforeEach(async ({ page }) => {
  demo = new PaymentsDemoPage(page);
  await demo.open();
});

test('shows the three live panels and the banks transaction-service accepts', async () => {
  await expect(demo.transactions).toBeVisible();
  await expect(demo.riskScreening).toBeVisible();
  await expect(demo.ledger).toBeVisible();
  await expect(demo.field('Source bank').getByRole('option')).toHaveCount(7);
});

test('a sent payment is published to Kafka and reaches every panel', async () => {
  const payer = uniqueName('Payer');
  const payee = uniqueName('Payee');

  await demo.fillPayment({ payer, payee, amount: '42.00' });
  await demo.send();

  await expect(demo.resultValue('Topic')).toHaveText('transactions.created');
  await expect(demo.resultValue('Key')).toHaveText(payer.toLowerCase());
  const position = `partition ${await demo.resultValue('Partition').textContent()}, offset ${await demo.resultValue('Offset').textContent()}`;
  await expect(demo.row(demo.transactions, payer, payee)).toContainText(position);
  await expect(demo.row(demo.riskScreening, payer, payee)).toContainText('LOW 0');
  await expect(demo.accountRow(payer)).toContainText('-$42.00');
  await expect(demo.accountRow(payee)).toContainText('$42.00');
  await expect(demo.ledger.getByText('Balanced', { exact: true })).toBeVisible();
});

test('a demo scenario fills the form, and fraud-service flags the self-payment', async () => {
  await demo.chooseScenario('Self-payment');
  await expect(demo.field('Payee')).toHaveValue('Mallory Black');
  await expect(demo.field('Amount (USD)')).toHaveValue('9500.00');

  const party = uniqueName('Self Payer');
  await demo.field('Payer').fill(party);
  await demo.field('Payee').fill(party);
  await demo.send();

  const assessment = demo.row(demo.riskScreening, party, party);
  await expect(assessment).toContainText('HIGH 95');
  await expect(assessment.getByRole('listitem')).toHaveText([
    'Possible structuring +30',
    'Self payment +50',
    'New payee high value +15',
  ]);
  await expect(demo.accountRow(party)).toContainText('$0.00');
});

test('the fourth quick payment by one payer is flagged for velocity', async () => {
  await demo.chooseScenario('Velocity burst');
  const payer = uniqueName('Quick Payer');
  await demo.field('Payer').fill(payer);

  for (let payment = 1; payment <= 4; payment++) {
    await demo.send();
  }

  const payments = demo.row(demo.riskScreening, payer, 'Coffee Shop');
  await expect(payments).toHaveCount(4);
  await expect(payments.filter({ hasText: 'High velocity +30' })).toHaveCount(1);
  await expect(payments.first()).toContainText('MEDIUM 30');
});

test('checks the form before anything is sent', async () => {
  await demo.send();

  await expect(demo.field('Payer')).toHaveAccessibleDescription('Use between 2 and 100 characters.');
  await expect(demo.field('Amount (USD)')).toHaveAttribute('aria-invalid', 'true');
  await expect(demo.result).toHaveCount(0);
});
