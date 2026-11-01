import { useEffect, useState } from 'react';

export const POLL_INTERVAL_MS = 2000;

export interface PollingState<T> {
  data?: T;
  error?: Error;
}

/**
 * Calls `load` straight away and then on every interval. A failed call keeps the last data it
 * had, so a panel goes on showing it while a service is down.
 */
export function usePolling<T>(load: () => Promise<T>, intervalMs = POLL_INTERVAL_MS): PollingState<T> {
  const [state, setState] = useState<PollingState<T>>({});
  useEffect(() => {
    let active = true;
    const poll = () =>
      load()
        .then((data) => active && setState({ data }))
        .catch((error: Error) => active && setState((previous) => ({ data: previous.data, error })));
    poll();
    const timer = setInterval(poll, intervalMs);
    return () => {
      active = false;
      clearInterval(timer);
    };
  }, [load, intervalMs]);
  return state;
}
