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