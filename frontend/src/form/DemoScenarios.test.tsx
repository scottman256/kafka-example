import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { DemoScenarios } from './DemoScenarios';
import { SCENARIOS } from './scenarios';

describe('DemoScenarios', () => {
  it('lists every scenario with what it should trigger', () => {
    render(<DemoScenarios onSelect={vi.fn()} />);

    expect(screen.getAllByRole('button')).toHaveLength(SCENARIOS.length);
    expect(screen.getByText('Send it 4 times: the 4th is flagged')).toBeInTheDocument();
  });

  it('fills the form with the chosen scenario', async () => {
    const onSelect = vi.fn();
    render(<DemoScenarios onSelect={onSelect} />);

    await userEvent.click(screen.getByRole('button', { name: 'Self-payment' }));

    expect(onSelect).toHaveBeenCalledWith(
      expect.objectContaining({ payer: 'Mallory Black', payee: 'Mallory Black', amount: '9500.00' }),
    );
  });
});
