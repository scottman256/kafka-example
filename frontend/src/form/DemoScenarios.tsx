import type { TransactionDraft } from './draft';
import { SCENARIOS, scenarioDraft } from './scenarios';

interface DemoScenariosProps {
  onSelect: (draft: TransactionDraft) => void;
}

export function DemoScenarios({ onSelect }: DemoScenariosProps) {
  return (
    <section className="card scenarios" aria-labelledby="scenarios-heading">
      <h2 id="scenarios-heading">Demo scenarios</h2>
      <p className="hint">Fill the form with a case that trips a fraud rule, then send it.</p>
      <ul>
        {SCENARIOS.map((scenario) => (
          <li key={scenario.name}>
            <button type="button" onClick={() => onSelect(scenarioDraft(scenario, new Date()))}>
              {scenario.name}
            </button>
            <span>{scenario.expectation}</span>
          </li>
        ))}
      </ul>
    </section>
  );
}
