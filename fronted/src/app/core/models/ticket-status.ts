export type TicketStatus = 'PENDING' | 'IN_PROGRESS' | 'WAITING_FOR_PARTS' | 'COMPLETED' | 'DELIVERED' | 'CANCELLED';

export const TICKET_STATUS_LABELS: Record<TicketStatus, string> = {
  PENDING: 'Pending',
  IN_PROGRESS: 'In progress',
  WAITING_FOR_PARTS: 'Waiting for parts',
  COMPLETED: 'Completed',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled'
};

export const TICKET_STATUS_SEVERITY: Record<TicketStatus, 'warn' | 'info' | 'success' | 'danger'> = {
  PENDING: 'warn',
  IN_PROGRESS: 'info',
  WAITING_FOR_PARTS: 'warn',
  COMPLETED: 'success',
  DELIVERED: 'success',
  CANCELLED: 'danger'
};

// UI help only. The backend owns the rule and answers 400 when the note is missing.
export const STATUS_NOTE: Partial<Record<TicketStatus, { label: string; required: boolean }>> = {
  COMPLETED: { label: 'Repair summary', required: true },
  CANCELLED: { label: 'Cancellation reason', required: true },
};

export const DEFAULT_STATUS_NOTE = { label: 'Note (optional)', required: false };