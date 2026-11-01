import type { Transaction } from '../api/transactions';
import type { ResultOutcome } from './submit';

export function SubmitResult({ outcome }: { outcome?: ResultOutcome }) {
  if (outcome === undefined) {
    return <p className="card hint">Send a transaction to see where it landed in Kafka.</p>;
  }
  if (outcome.status === 'failed') {
    return (
      <div className="card result result--failed" role="alert">
        {outcome.message}
      </div>
    );
  }
  return <PublishedResult transaction={outcome.transaction} />;
}

function PublishedResult({ transaction }: { transaction: Transaction }) {
  // A 201 from transaction-service always carries the broker's acknowledgement.
  const kafka = transaction.kafka!;
  return (
    <section className="card result" aria-labelledby="result-heading">
      <h2 id="result-heading">Published to Kafka</h2>
      <dl>
        <dt>Transaction</dt>
        <dd>{transaction.id}</dd>
        <dt>Topic</dt>
        <dd>{kafka.topic}</dd>
        <dt>Partition</dt>
        <dd>{kafka.partition}</dd>
        <dt>Offset</dt>
        <dd>{kafka.offset}</dd>
        <dt>Key</dt>
        <dd>{kafka.key}</dd>
      </dl>
    </section>
  );
}
