import { TicketStatus } from './ticket-status';

export type TimelineEntry =
  | { type: 'STATUS_CHANGE'; occurredAt: string; authorName: string; previousStatus: TicketStatus | null; newStatus: TicketStatus; note: string | null }
  | { type: 'NOTE'; occurredAt: string; authorName: string; text: string };
