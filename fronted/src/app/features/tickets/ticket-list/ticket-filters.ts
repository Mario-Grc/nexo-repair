import type { ParamMap, Params } from '@angular/router';
import type { EmployeeRole } from '../../../core/models/employee';
import type { TicketStatus } from '../../../core/models/ticket-status';
import { OPEN_STATUSES, type TicketFilters } from '../../../core/models/ticket-filters';

export { OPEN_STATUSES, type TicketFilters };

const VALID_STATUSES: readonly TicketStatus[] = [
  'PENDING',
  'IN_PROGRESS',
  'WAITING_FOR_PARTS',
  'COMPLETED',
  'DELIVERED',
  'CANCELLED',
];

const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

function isTicketStatus(value: string): value is TicketStatus {
  return (VALID_STATUSES as readonly string[]).includes(value);
}

function readDate(value: string | null): string | null {
  if (value === null) return null;
  const trimmed = value.trim();
  return DATE_PATTERN.test(trimmed) ? trimmed : null;
}

function readPage(value: string | null): number {
  if (value === null) return 0;
  const parsed = Number.parseInt(value, 10);
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : 0;
}

function readSize(value: string | null): number {
  if (value === null) return 20;
  const parsed = Number.parseInt(value, 10);
  if (!Number.isInteger(parsed) || parsed <= 0) return 20;
  return Math.min(parsed, 100);
}

// Build filters from the URL. Unknown status values are dropped.
export function filtersFromParams(paramMap: ParamMap): TicketFilters {
  const status = paramMap.getAll('status').filter(isTicketStatus);

  let assignee: TicketFilters['assignee'] = null;
  if (paramMap.get('unassigned') === 'true') {
    assignee = 'unassigned';
  } else {
    const rawAssignee = paramMap.get('assignedEmployeeId');
    if (rawAssignee !== null) {
      const parsed = Number.parseInt(rawAssignee, 10);
      if (Number.isInteger(parsed) && parsed > 0) assignee = parsed;
    }
  }

  const rawQuery = paramMap.get('q');

  return {
    status,
    assignee,
    createdFrom: readDate(paramMap.get('createdFrom')),
    createdTo: readDate(paramMap.get('createdTo')),
    q: rawQuery === null ? '' : rawQuery.trim(),
    page: readPage(paramMap.get('page')),
    size: readSize(paramMap.get('size')),
  };
}

// Build URL params from filters. Page and size are always present.
// This keeps the URL non empty after the user edits filters.
export function paramsFromFilters(filters: TicketFilters): Params {
  const params: Params = {};

  if (filters.status.length > 0) params['status'] = [...filters.status];

  if (filters.assignee === 'unassigned') {
    params['unassigned'] = 'true';
  } else if (typeof filters.assignee === 'number') {
    params['assignedEmployeeId'] = String(filters.assignee);
  }

  if (filters.createdFrom !== null) params['createdFrom'] = filters.createdFrom;
  if (filters.createdTo !== null) params['createdTo'] = filters.createdTo;

  const q = filters.q.trim();
  if (q !== '') params['q'] = q;

  params['page'] = String(filters.page);
  params['size'] = String(filters.size);

  return params;
}

// Default list view. Only technicians start scoped to their own tickets.
export function defaultFilters(role: EmployeeRole | null, myId: number | null): TicketFilters {
  return {
    status: [...OPEN_STATUSES],
    assignee: role === 'TECHNICIAN' && myId !== null ? myId : null,
    createdFrom: null,
    createdTo: null,
    q: '',
    page: 0,
    size: 20,
  };
}

// Local date as yyyy-MM-dd. It uses local getters on purpose.
// toISOString works in UTC and moves midnight back one day in summer time.
export function toIsoDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}
