import { afterEach, describe, expect, it, vi } from 'vitest';
import { listPath, request, tokenExpiry } from './api';
afterEach(() => vi.unstubAllGlobals());
describe('API contract', () => {
  it('uses zero-based pagination and stable sorting on search', () => {
    const url = new URL(listPath(false, { mode: 'search', title: 'Coffee', minAmount: '5' }, 2, 10, 'expenseDate,desc'), 'http://localhost');
    expect(url.pathname).toBe('/expenses/search');
    expect(url.searchParams.get('page')).toBe('2');
    expect(url.searchParams.getAll('sort')).toEqual(['expenseDate,desc', 'id,desc']);
    expect(url.searchParams.get('title')).toBe('Coffee');
    expect(url.searchParams.has('minAmount')).toBe(false);
  });
  it('keeps recurring range filters separate from search', () => {
    const url = new URL(listPath(true, { mode: 'filter', minAmount: '10', maxAmount: '100', title: 'Rent', frequency: 'MONTHLY' }, 0, 20, 'amount,asc'), 'http://localhost');
    expect(url.pathname).toBe('/recurring-expenses/filter');
    expect(url.searchParams.get('minAmount')).toBe('10');
    expect(url.searchParams.has('frequency')).toBe(false);
    expect(url.searchParams.has('title')).toBe(false);
  });
  it('reads expiry and rejects malformed tokens', () => {
    expect(tokenExpiry(`header.${btoa(JSON.stringify({ exp: 12345 }))}.signature`)).toBe(12345000);
    expect(tokenExpiry('invalid')).toBe(0);
    expect(tokenExpiry(`header.${btoa('{}')}.signature`)).toBe(0);
  });
  it('sends bearer authentication and handles empty deletes', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal('fetch', fetchMock);
    expect(await request('/expenses/4', 'jwt', { method: 'DELETE' })).toBeUndefined();
    expect(fetchMock.mock.calls[0][0]).toBe('/api/expenses/4');
    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe('Bearer jwt');
  });
  it('preserves an empty security-filter 401', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 401 })));
    await expect(request('/expenses', 'expired')).rejects.toMatchObject({ status: 401, message: 'Please sign in again.' });
  });
  it('displays backend validation messages', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ message: 'amount: must be positive' }), { status: 400 })));
    await expect(request('/expenses')).rejects.toMatchObject({ status: 400, message: 'amount: must be positive' });
  });
});
