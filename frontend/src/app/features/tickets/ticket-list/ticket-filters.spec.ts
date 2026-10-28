import type { ParamMap } from '@angular/router';
import { describe, expect, it } from 'vitest';
import {
  OPEN_STATUSES,
  defaultFilters,
  filtersFromParams,
  paramsFromFilters,
  toIsoDate,
  type TicketFilters,
} from './ticket-filters';

// Minimal ParamMap stub built from plain params.
// It mirrors how the router exposes repeated status values.
function paramMapFrom(params: Record<string, string | string[]>): ParamMap {
  const normalized = new Map<string, string[]>();
  for (const [key, value] of Object.entries(params)) {
    normalized.set(key, Array.isArray(value) ? value : [value]);
  }
  return {
    has: (name: string) => normalized.has(name),
    get: (name: string) => normalized.get(name)?.[0] ?? null,
    getAll: (name: string) => normalized.get(name) ?? [],
    get keys(): string[] {
      return [...normalized.keys()];
    },
  } as ParamMap;
}

// Params from the router use strings, so normalize before building the stub.
function paramMapFromRouterParams(params: Record<string, unknown>): ParamMap {
  const record: Record<string, string | string[]> = {};
  for (const [key, value] of Object.entries(params)) {
    if (Array.isArray(value)) record[key] = value.map(String);
    else if (value !== undefined) record[key] = String(value);
  }
  return paramMapFrom(record);
}

describe('ticket filters', () => {
  it('roundtrips several statuses with an unassigned filter', () => {
    const filters: TicketFilters = {
      status: ['PENDING', 'IN_PROGRESS', 'DELIVERED'],
      assignee: 'unassigned',
      createdFrom: '2026-07-01',
      createdTo: '2026-07-12',
      q: 'acme',
      page: 2,
      size: 20,
    };

    const back = filtersFromParams(paramMapFromRouterParams(paramsFromFilters(filters)));

    expect(back).toEqual(filters);
  });

  it('roundtrips a concrete technician with free text', () => {
    const filters: TicketFilters = {
      status: ['COMPLETED'],
      assignee: 7,
      createdFrom: null,
      createdTo: null,
      q: '50% tv',
      page: 0,
      size: 20,
    };

    const params = paramsFromFilters(filters);

    expect(params['assignedEmployeeId']).toBe('7');
    expect(params['unassigned']).toBeUndefined();
    expect(filtersFromParams(paramMapFromRouterParams(params))).toEqual(filters);
  });

  it('keeps an empty filter set in the URL through page and size', () => {
    const filters: TicketFilters = {
      status: [],
      assignee: null,
      createdFrom: null,
      createdTo: null,
      q: '',
      page: 0,
      size: 20,
    };

    const params = paramsFromFilters(filters);

    expect(params['page']).toBe('0');
    expect(params['size']).toBe('20');
    expect(filtersFromParams(paramMapFromRouterParams(params))).toEqual(filters);
  });

  it('gives assignee only to technicians', () => {
    expect(defaultFilters('TECHNICIAN', 9).assignee).toBe(9);
    expect(defaultFilters('TECHNICIAN', 9).status).toEqual(OPEN_STATUSES);
    expect(defaultFilters('RECEPTION', 9).assignee).toBeNull();
    expect(defaultFilters('ADMIN', 9).assignee).toBeNull();
    expect(defaultFilters(null, null).assignee).toBeNull();
    expect(defaultFilters('TECHNICIAN', null).assignee).toBeNull();
  });

  it('starts from open tickets on page zero', () => {
    const defaults = defaultFilters(null, null);

    expect(defaults.page).toBe(0);
    expect(defaults.size).toBe(20);
    expect(defaults.q).toBe('');
    expect(defaults.createdFrom).toBeNull();
    expect(defaults.createdTo).toBeNull();
  });

  it('formats a local date without UTC shifts', () => {
    expect(toIsoDate(new Date(2026, 6, 12, 0, 30))).toBe('2026-07-12');
  });
});
