import { act, renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { POLL_INTERVAL_MS, usePolling } from './usePolling';

describe('usePolling', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  it('loads straight away and then on every interval', async () => {
    const load = vi.fn().mockResolvedValue('latest');

    const { result } = renderHook(() => usePolling(load));
    await act(() => vi.advanceTimersByTimeAsync(2 * POLL_INTERVAL_MS));

    expect(result.current).toEqual({ data: 'latest' });
    expect(load).toHaveBeenCalledTimes(3);
  });

  it('keeps the last data and reports the error when a call fails', async () => {
    const load = vi.fn().mockResolvedValueOnce('first').mockRejectedValue(new Error('service down'));

    const { result } = renderHook(() => usePolling(load, 1000));
    await act(() => vi.advanceTimersByTimeAsync(1000));

    expect(result.current.data).toBe('first');
    expect(result.current.error?.message).toBe('service down');
  });

  it('clears the error once a call succeeds again', async () => {
    const load = vi.fn().mockRejectedValueOnce(new Error('service down')).mockResolvedValue('back');

    const { result } = renderHook(() => usePolling(load, 1000));
    await act(() => vi.advanceTimersByTimeAsync(1000));

    expect(result.current).toEqual({ data: 'back' });
  });

  it('stops polling and ignores late answers once unmounted', async () => {
    let answer: (value: string) => void = () => {};
    let fail: (error: Error) => void = () => {};
    const load = vi
      .fn()
      .mockReturnValueOnce(new Promise((resolve) => (answer = resolve)))
      .mockReturnValueOnce(new Promise((_, reject) => (fail = reject)));
    const { result, unmount } = renderHook(() => usePolling(load, 1000));
    await act(() => vi.advanceTimersByTimeAsync(1000));

    unmount();
    await act(async () => {
      answer('too late');
      fail(new Error('too late'));
      await vi.advanceTimersByTimeAsync(5000);
    });

    expect(load).toHaveBeenCalledTimes(2);
    expect(result.current).toEqual({});
  });
});
