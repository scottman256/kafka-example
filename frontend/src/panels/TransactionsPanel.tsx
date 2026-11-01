import { fetchRecentTransactions, type KafkaPublication, type Transaction } from '../api/transactions';
import { formatMoney } from '../format';
import { usePolling } from '../usePolling';
import { Panel, RowList } from './Panel';

export function TransactionsPanel() {
  const state = usePolling(fetchRecentTransactions);
  return (
    <Panel title="Transactions" service="transaction-service" state={state}>
      {(transactions) => (
        <RowList items={transactions} empty="No transactions yet.">
          {(transaction) => <TransactionRow key={transaction.id} transaction={transaction} />}
        </RowList>
      )}
    </Panel>
  );
}

function TransactionRow({ transaction }: { transaction: Transaction }) {
  return (
    <li className="row">
      <span className="row__parties">
        {transaction.payer} → {transaction.payee}
      </span>
      <span className="row__amount">{formatMoney(transaction.amount)}</span>
      <span className="row__meta">
        {transaction.transactionDate} · {transaction.sourceBank} · {kafkaPosition(transaction.kafka)}
      </span>
    </li>
  );
}

function kafkaPosition(kafka: KafkaPublication | null): string {
  if (kafka === null || kafka.status !== 'PUBLISHED') {
    return 'not published to Kafka';
  }
  return `partition ${kafka.partition}, offset ${kafka.offset}`;
}
