import { TicketStatus } from '../../../core/models/ticket-status';

// Linear display order: parts are expected before work begins, while the
// backend state machine still allows moving between IN_PROGRESS and
// WAITING_FOR_PARTS. CANCELLED is not part of the flow, so indexOf gives -1.
export const STEPPER_ORDER: TicketStatus[] = ['PENDING', 'WAITING_FOR_PARTS', 'IN_PROGRESS', 'COMPLETED', 'DELIVERED'];

export function getStepperIndex(status: TicketStatus): number {
  return STEPPER_ORDER.indexOf(status);
}
