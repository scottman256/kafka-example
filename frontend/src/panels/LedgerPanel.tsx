import { fetchLedgerSnapshot, type Account, type BankSummary, type TrialBalance } from '../api/ledger';
import { formatMoney } from '../format';
import { usePolling } from '../usePolling';
import { Panel } from './Panel';

export function LedgerPanel() {
  const state = usePolling(fetchLedgerSnapshot);
  return (
    <Panel title="Ledger" service="ledger-service" state={state}>
      {(ledger) =>
        ledger.accounts.length === 0 ? (
          <p className="panel__empty">Nothing posted yet.</p>
        ) : (
          <>
            <TrialBalanceSummary trialBalance={ledger.trialBalance} />
            <AccountTable accounts={ledger.accounts} />
            <BankTable banks={ledger.banks} />
          </>
        )
      }
    </Panel>
  );
}

function TrialBalanceSummary({ trialBalance }: { trialBalance: TrialBalance }) {
  const { balanced, totalDebits, totalCredits, accountCount } = trialBalance;
  return (
    <p className={`trial-balance trial-balance--${balanced ? 'balanced' : 'unbalanced'}`}>
      <strong>{balanced ? 'Balanced' : 'Out of balance'}</strong>: debits {formatMoney(totalDebits)}, credits{' '}
      {formatMoney(totalCredits)} across {accountCount} accounts
    </p>
  );
}

function AccountTable({ accounts }: { accounts: Account[] }) {
  return (
    <table>
      <caption>Account balances</caption>
      <thead>
        <tr><th>Account</th><th>Balance</th><th>Entries</th></tr>
      </thead>
      <tbody>
        {accounts.map((account) => (
          <tr key={account.name}>
            <td>{account.name}</td>
            <td className={account.balance < 0 ? 'amount--negative' : 'amount--positive'}>{formatMoney(account.balance)}</td>
            <td>{account.entryCount}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function BankTable({ banks }: { banks: BankSummary[] }) {
  return (
    <table>
      <caption>Outflow by source bank</caption>
      <thead>
        <tr><th>Bank</th><th>Paid out</th><th>Payments</th></tr>
      </thead>
      <tbody>
        {banks.map((bank) => (
          <tr key={bank.bankName}>
            <td>{bank.bankName}</td>
            <td>{formatMoney(bank.totalOutflow)}</td>
            <td>{bank.transactionCount}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
