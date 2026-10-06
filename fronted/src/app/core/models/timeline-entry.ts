import { TicketStatus } from './ticket-status';

export type TimelineEntry =
  | { type: 'STATUS_CHANGE'; occurredAt: string; authorName: string; previousStatus: TicketStatus | null; newStatus: TicketStatus; note: string | null }
  | { type: 'NOTE'; occurredAt: string; authorName: string; text: string }
  | { type: 'ASSIGNMENT_CHANGE'; occurredAt: string; authorName: string; previousEmployeeName: string | null; newEmployeeName: string | null }
  | { type: 'PART_ADDED'; occurredAt: string; authorName: string; description: string; quantity: number }
  | { type: 'PART_REMOVED'; occurredAt: string; authorName: string; description: string; quantity: number };
