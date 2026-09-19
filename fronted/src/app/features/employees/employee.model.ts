export interface Employee {
  id: number;
  name: string;
}

export type NewEmployee = Omit<Employee, 'id'>;
