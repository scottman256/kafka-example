import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { PANEL_ROWS, Panel, RowList } from './Panel';

describe('Panel', () => {
  it('shows a loading message until the first data arrives', () => {
    render(
      <Panel title="Transactions" service="transaction-service" state={{}}>
        {() => <p>content</p>}
      </Panel>,
    );

    expect(screen.getByText('Loading…')).toBeInTheDocument();
    expect(screen.queryByText('content')).not.toBeInTheDocument();
  });

  it('renders the data under the title and service name', () => {
    render(
      <Panel title="Transactions" service="transaction-service" state={{ data: 'latest' }}>
        {(data) => <p>{data}</p>}
      </Panel>,
    );

    expect(screen.getByRole('region', { name: 'Transactions' })).toHaveTextContent('transaction-service');
    expect(screen.getByText('latest')).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('warns when the service is unreachable but keeps the last data on screen', () => {
    render(
      <Panel title="Transactions" service="transaction-service" state={{ data: 'last', error: new Error('down') }}>
        {(data) => <p>{data}</p>}
      </Panel>,
    );

    expect(screen.getByRole('alert')).toHaveTextContent("Can't reach transaction-service right now.");
    expect(screen.getByText('last')).toBeInTheDocument();
  });
});

describe('RowList', () => {
  it('shows the empty message when there are no items', () => {
    render(<RowList items={[]} empty="Nothing yet.">{() => null}</RowList>);

    expect(screen.getByText('Nothing yet.')).toBeInTheDocument();
  });

  it(`shows at most ${PANEL_ROWS} rows`, () => {
    const items = Array.from({ length: PANEL_ROWS + 2 }, (_, index) => index);

    render(<RowList items={items} empty="Nothing yet.">{(item) => <li key={item}>{item}</li>}</RowList>);

    expect(screen.getAllByRole('listitem')).toHaveLength(PANEL_ROWS);
  });
});
