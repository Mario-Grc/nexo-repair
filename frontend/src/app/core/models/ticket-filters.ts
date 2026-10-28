import type { TicketStatus } from './ticket-status';

// Tickets that still count as open for the default list view.
export const OPEN_STATUSES: TicketStatus[] = ['PENDING', 'IN_PROGRESS', 'WAITING_FOR_PARTS', 'COMPLETED'];

export interface TicketFilters {
  status: TicketStatus[];
  assignee: null | 'unassigned' | number;
  createdFrom: string | null;
  createdTo: string | null;
  q: string;
  page: number;
  size: number;
}
