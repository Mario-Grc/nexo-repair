import { Routes } from '@angular/router';
import { EmployeeList } from './features/employees/employee-list/employee-list';
import { TicketList } from './features/tickets/ticket-list/ticket-list';
import { TicketDetail } from './features/tickets/ticket-detail/ticket-detail';
import { CustomerList } from './features/customers/customer-list/customer-list';
import { employeeSessionGuard } from './core/guards/employee-session.guard';
import { AppShellComponent } from './layout/app-shell.component';
import { SelectEmployeeComponent } from './features/auth/select-employee.component';

export const routes: Routes = [
    { path: 'select-employee', component: SelectEmployeeComponent },
    {
        path: '',
        component: AppShellComponent,
        canActivate: [employeeSessionGuard],
        children: [
            { path: 'tickets', component: TicketList },
            { path: 'tickets/:publicId', component: TicketDetail },
            { path: 'customers', component: CustomerList },
            { path: 'employees', component: EmployeeList },
            { path: '', redirectTo: 'tickets', pathMatch: 'full' }
        ]
    }
];
