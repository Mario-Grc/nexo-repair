export interface Customer {
  id: number;
  name: string;
  email: string | null;
  phone: string | null;
  notes: string | null;
}

export type NewCustomer = Omit<Customer, 'id'>;