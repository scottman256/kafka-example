import { expect, type Locator, type Page } from '@playwright/test';

export interface PaymentDetails {
  payer: string;
  payee: string;
  amount: string;
  memo?: string;
  sourceBank?: string;
}

/** The demo's single page, described the way a user sees it. */
export class PaymentsDemoPage {
  readonly result: Locator;
  readonly transactions: Locator;
  readonly riskScreening: Locator;
  readonly ledger: Locator;

  constructor(private readonly page: Page) {
    this.result = page.getByRole('region', { name: 'Published to Kafka' });
    this.transactions = page.getByRole('region', { name: 'Transactions' });
    this.riskScreening = page.getByRole('region', { name: 'Risk screening' });
    this.ledger = page.getByRole('region', { name: 'Ledger' });
  }

  field(label: string): Locator {
    return this.page.getByLabel(label);
  }

  /** Opens the page and waits for the bank list to load from transaction-service. */
  async open(): Promise<void> {
    await this.page.goto('/');
    await expect(this.field('Source bank').getByRole('option', { name: 'First National Bank' })).toBeAttached();
  }

  async fillPayment({ payer, payee, amount, memo = 'Playwright test', sourceBank = 'Summit Credit Union' }: PaymentDetails) {
    await this.field('Payer').fill(payer);
    await this.field('Payee').fill(payee);
    await this.field('Amount (USD)').fill(amount);
    await this.field('Memo').fill(memo);
    await this.field('Source bank').selectOption(sourceBank);
  }

  async chooseScenario(name: string): Promise<void> {
    await this.page.getByRole('button', { name }).click();
  }

  /** Sends the form and waits until transaction-service has answered. */
  async send(): Promise<void> {
    const sendButton = this.page.getByRole('button', { name: 'Send transaction' });
    await sendButton.click();
    await expect(sendButton).toBeEnabled();
  }

  /** The value next to a label in the result card, such as "Offset". */
  resultValue(label: string): Locator {
    return this.result.locator(`dt:text-is("${label}") + dd`);
  }

  /** A panel's row for a payment from payer to payee. */
  row(panel: Locator, payer: string, payee: string): Locator {
    return panel.getByRole('listitem').filter({ hasText: `${payer} → ${payee}` });
  }

  accountRow(name: string): Locator {
    return this.ledger.getByRole('table', { name: 'Account balances' }).getByRole('row', { name });
  }
}
