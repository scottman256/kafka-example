import { useState } from 'react';
import { DemoScenarios } from './form/DemoScenarios';
import { emptyDraft } from './form/draft';
import { SubmitResult } from './form/SubmitResult';
import type { ResultOutcome } from './form/submit';
import { TransactionForm } from './form/TransactionForm';
import { LedgerPanel } from './panels/LedgerPanel';
import { RiskPanel } from './panels/RiskPanel';
import { TransactionsPanel } from './panels/TransactionsPanel';

export function App() {
  const [draft, setDraft] = useState(() => emptyDraft(new Date()));
  const [outcome, setOutcome] = useState<ResultOutcome>();
  return (
    <div className="app">
      <header className="app__header">
        <h1>Kafka Payments Demo</h1>
        <p>Each transaction is published once to Kafka, then screened and posted by two independent consumers.</p>
      </header>
      <main>
        <div className="intake">
          <TransactionForm draft={draft} onDraftChange={setDraft} onOutcome={setOutcome} />
          <div className="intake__side">
            <SubmitResult outcome={outcome} />
            <DemoScenarios onSelect={setDraft} />
          </div>
        </div>
        <div className="panels">
          <TransactionsPanel />
          <RiskPanel />
          <LedgerPanel />
        </div>
      </main>
    </div>
  );
}
