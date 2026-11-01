import { vi } from 'vitest';

export interface FakeResponse {
  status?: number;
  body?: unknown;
}

export type FakeRoutes = Record<string, FakeResponse>;

/** Replaces fetch with canned responses keyed by "METHOD /path". Anything else is a 404. */
export function fakeApi(routes: FakeRoutes) {
  const fetch = vi.fn(async (url: string, init?: RequestInit) => {
    const route = routes[`${init?.method ?? 'GET'} ${url}`];
    if (route === undefined) {
      return new Response(null, { status: 404 });
    }
    return Response.json(route.body ?? null, { status: route.status ?? 200 });
  });
  vi.stubGlobal('fetch', fetch);
  return fetch;
}

export function unreachableApi() {
  vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));
}
