export interface Customer {
    id: number;
    name: string;
    contact: string;
}

export type NewCustomer = Omit<Customer, 'id'>;