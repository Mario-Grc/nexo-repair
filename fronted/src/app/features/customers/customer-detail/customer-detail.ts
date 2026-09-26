import { Component, OnInit, ViewEncapsulation, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { MessageModule } from 'primeng/message';
import { TagModule } from 'primeng/tag';
import { CustomerService } from '../../../core/services/customer.service';
import { TicketService } from '../../../core/services/ticket.service';
import { CustomerFormDialog } from '../customer-form-dialog/customer-form-dialog';
import { Customer } from '../../../core/models/customer';
import { Ticket } from '../../../core/models/ticket';
import { TICKET_STATUS_LABELS, TICKET_STATUS_SEVERITY, TicketStatus } from '../../../core/models/ticket-status';

@Component({
  selector: 'app-customer-detail',
  imports: [DatePipe, RouterLink, TableModule, ButtonModule, CardModule, MessageModule, TagModule, CustomerFormDialog],
  templateUrl: './customer-detail.html',
  styleUrl: './customer-detail.css',
  // Same as TicketDetail: theme PrimeNG internals; rules are scoped under .customer-detail.
  encapsulation: ViewEncapsulation.None,
})
export class CustomerDetail implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private customerService = inject(CustomerService);
  private ticketService = inject(TicketService);

  customer = signal<Customer | null>(null);
  tickets = signal<Ticket[]>([]);
  error = signal<string | null>(null);
  dialogVisible = signal(false);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isInteger(id) || id <= 0) {
      this.error.set('Customer not found.');
      return;
    }
    this.customerService.getCustomer(id).subscribe({
      next: c => this.customer.set(c),
      error: () => this.error.set('Customer not found.'),
    });
    this.ticketService.getTickets(id).subscribe({
      next: list => this.tickets.set(list),
      error: () => this.tickets.set([]),
    });
  }

  goBack(): void {
    this.router.navigate(['/customers']);
  }

  onSaved(updated: Customer): void {
    this.customer.set(updated);
  }

  openTicket(ticket: Ticket): void {
    this.router.navigate(['/tickets', ticket.publicId]);
  }

  getStatusLabel(status: string): string {
    return TICKET_STATUS_LABELS[status as TicketStatus] ?? status;
  }

  getStatusSeverity(status: string): 'warn' | 'info' | 'success' | 'danger' {
    return TICKET_STATUS_SEVERITY[status as TicketStatus] ?? 'info';
  }
}
