import { Routes } from '@angular/router';
import { CustomerList } from './features/customers/customer-list/customer-list';
import { EmployeeList } from './features/employees/employee-list/employee-list';
import { TicketList } from './features/tickets/ticket-list/ticket-list';

export const routes: Routes = [
    {
        path: 'customers',
        component: CustomerList,
    },
    {
        path: 'employees',
        component: EmployeeList,
    },
    {
        path: 'tickets',
        component: TicketList,
    }
];
