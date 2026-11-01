import { describe, expect, it } from 'vitest';
import { fakeApi } from '../test/fakeApi';
import { ApiError, getJson, postJson } from './http';

describe('getJson', () => {
  it('returns the parsed body of a successful response', async () => {
    fakeApi({ 'GET /api/things': { body: ['one', 'two'] } });

    await expect(getJson('/api/things')).resolves.toEqual(['one', 'two']);
  });

  it('throws an ApiError carrying the problem detail of a failed response', async () => {
    fakeApi({ 'GET /api/things': { status: 400, body: { detail: 'Bad input.', fieldErrors: { payer: 'required' } } } });

    const error = await getJson('/api/things').catch((thrown: unknown) => thrown);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ message: 'Bad input.', problem: { status: 400, fieldErrors: { payer: 'required' } } });
  });

  it('describes a failed response that has no problem detail by its status', async () => {
    fakeApi({});

    await expect(getJson('/api/missing')).rejects.toThrow('The request failed with status 404.');
  });
});

describe('postJson', () => {
  it('sends the body as JSON and returns the parsed response', async () => {
    const fetch = fakeApi({ 'POST /api/things': { status: 201, body: { id: 'thing-1' } } });

    await expect(postJson('/api/things', { name: 'Thing' })).resolves.toEqual({ id: 'thing-1' });
    expect(fetch).toHaveBeenCalledWith('/api/things', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: '{"name":"Thing"}',
    });
  });
});
