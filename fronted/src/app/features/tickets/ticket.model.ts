export interface Ticket {
  id: number;
  description: string;
  deviceInfo: string;
  status: string;
  customerId: number;
  employeeId: number | null;
}
