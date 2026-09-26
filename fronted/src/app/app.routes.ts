import { Routes } from '@angular/router';
import { EmployeeList } from './features/employees/employee-list/employee-list';
import { TicketList } from './features/tickets/ticket-list/ticket-list';
import { TicketDetail } from './features/tickets/ticket-detail/ticket-detail';
import { CustomerList } from './features/customers/customer-list/customer-list';
import { CustomerDetail } from './features/customers/customer-detail/customer-detail';
import { authGuard } from './core/guards/auth.guard';
import { AppShellComponent } from './layout/app-shell';
import { LoginComponent } from './features/auth/login';

export const routes: Routes = [
    { path: 'login', component: LoginComponent },
    {
        path: '',
        component: AppShellComponent,
        canActivate: [authGuard],
        children: [
            { path: 'tickets', component: TicketList },
            { path: 'tickets/:publicId', component: TicketDetail },
            { path: 'customers', component: CustomerList },
            { path: 'customers/:id', component: CustomerDetail },
            { path: 'employees', component: EmployeeList },
            { path: '', redirectTo: 'tickets', pathMatch: 'full' }
        ]
    }
];
