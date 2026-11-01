import { fetchRecentAssessments, type RiskAssessment, type TriggeredRule } from '../api/risk';
import { formatMoney, humanize } from '../format';
import { usePolling } from '../usePolling';
import { Panel, RowList } from './Panel';

export function RiskPanel() {
  const state = usePolling(fetchRecentAssessments);
  return (
    <Panel title="Risk screening" service="fraud-service" state={state}>
      {(assessments) => (
        <RowList items={assessments} empty="Nothing screened yet.">
          {(assessment) => <AssessmentRow key={assessment.transactionId} assessment={assessment} />}
        </RowList>
      )}
    </Panel>
  );
}

function AssessmentRow({ assessment }: { assessment: RiskAssessment }) {
  return (
    <li className="row">
      <span className="row__parties">
        {assessment.payer} → {assessment.payee}
      </span>
      <span className={`badge badge--${assessment.riskLevel.toLowerCase()}`}>
        {assessment.riskLevel} {assessment.riskScore}
      </span>
      <span className="row__meta">{formatMoney(assessment.amount)}</span>
      <RuleList rules={assessment.triggeredRules} />
    </li>
  );
}

function RuleList({ rules }: { rules: TriggeredRule[] }) {
  if (rules.length === 0) {
    return <span className="row__meta">No rules triggered</span>;
  }
  return (
    <ul className="rules">
      {rules.map((rule) => (
        <li key={rule.rule} title={rule.description}>
          {humanize(rule.rule)} +{rule.points}
        </li>
      ))}
    </ul>
  );
}
