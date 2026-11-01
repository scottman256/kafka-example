import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { fakeApi } from '../test/fakeApi';
import { assessment, serviceRoutes } from '../test/fixtures';
import { RiskPanel } from './RiskPanel';

describe('RiskPanel', () => {
  it('shows each assessment with its colour-coded risk and the rules it triggered', async () => {
    fakeApi(serviceRoutes());
    render(<RiskPanel />);

    expect(await screen.findByText('HIGH 80')).toHaveClass('badge--high');
    expect(screen.getByText('Mallory Black → Mallory Black')).toBeInTheDocument();
    expect(screen.getByText('Self payment +50')).toHaveAttribute('title', 'Payer and payee are the same party');
    expect(screen.getByText('Possible structuring +30')).toBeInTheDocument();
  });

  it('says when an assessment triggered no rules', async () => {
    const lowRisk = { ...assessment, riskLevel: 'LOW', riskScore: 0, triggeredRules: [] };
    fakeApi({ 'GET /api/risk/assessments': { body: [lowRisk] } });
    render(<RiskPanel />);

    expect(await screen.findByText('LOW 0')).toHaveClass('badge--low');
    expect(screen.getByText('No rules triggered')).toBeInTheDocument();
  });

  it('says when nothing has been screened yet', async () => {
    fakeApi({ 'GET /api/risk/assessments': { body: [] } });
    render(<RiskPanel />);

    expect(await screen.findByText('Nothing screened yet.')).toBeInTheDocument();
  });
});
