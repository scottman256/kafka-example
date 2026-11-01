import { getJson } from './http';

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';

export interface TriggeredRule {
  rule: string;
  points: number;
  description: string;
}

export interface RiskAssessment {
  transactionId: string;
  payer: string;
  payee: string;
  amount: number;
  riskScore: number;
  riskLevel: RiskLevel;
  triggeredRules: TriggeredRule[];
}

export const fetchRecentAssessments = () => getJson<RiskAssessment[]>('/api/risk/assessments');
