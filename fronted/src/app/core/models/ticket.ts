import { DeviceType } from './device-type';
import { TicketStatus } from './ticket-status';

export interface Device {
  type: DeviceType;
  brand: string;
  model: string;
  identifier: string | null;
}

export interface Ticket {
  publicId: string;
  problemDescription: string;
  device: Device;
  status: TicketStatus;
  customerId: number;
  customerName: string;
  assignedEmployeeId: number | null;
  assignedEmployeeName: string | null;
  createdAt: string;
}

export interface NewTicket {
  problemDescription: string;
  device: Device;
  customerId: number;
  createdByEmployeeId: number;
}