import type { ReactNode } from 'react';
import type { PollingState } from '../usePolling';

export const PANEL_ROWS = 10;

interface PanelProps<T> {
  title: string;
  service: string;
  state: PollingState<T>;
  children: (data: T) => ReactNode;
}

export function Panel<T>({ title, service, state, children }: PanelProps<T>) {
  return (
    <section className="card panel" aria-label={title}>
      <header className="panel__header">
        <h2>{title}</h2>
        <span className="panel__service">{service}</span>
      </header>
      {state.error && (
        <p className="panel__error" role="alert">
          Can't reach {service} right now.
        </p>
      )}
      {state.data === undefined ? <p className="panel__empty">Loading…</p> : children(state.data)}
    </section>
  );
}

interface RowListProps<T> {
  items: T[];
  empty: string;
  children: (item: T) => ReactNode;
}

/** The newest rows first, up to PANEL_ROWS of them. */
export function RowList<T>({ items, empty, children }: RowListProps<T>) {
  if (items.length === 0) {
    return <p className="panel__empty">{empty}</p>;
  }
  return <ul className="rows">{items.slice(0, PANEL_ROWS).map(children)}</ul>;
}
