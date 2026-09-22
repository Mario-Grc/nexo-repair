export type EmployeeRole = 'TECHNICIAN' | 'RECEPTION' | 'ADMIN';

export interface Employee {
  id: number;
  name: string;
  email: string;
  role: EmployeeRole;
  active: boolean;
}