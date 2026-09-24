import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TicketService } from '../../../core/services/ticket.service';
import { CustomerService } from '../../../core/services/customer.service';
import { EmployeeSessionService } from '../../../core/services/employee-session.service';
import { Customer } from '../../../core/models/customer';
import { Ticket } from '../../../core/models/ticket';
import { DEVICE_TYPE_OPTIONS } from '../../../core/models/device-type';
import { TicketStatus, TICKET_STATUS_LABELS, TICKET_STATUS_SEVERITY } from '../../../core/models/ticket-status';

@Component({
  selector: 'app-ticket-list',
  imports: [TableModule, ButtonModule, DialogModule, InputTextModule, TextareaModule, SelectModule, TagModule, ReactiveFormsModule, DatePipe],
  templateUrl: './ticket-list.html',
})
export class TicketList implements OnInit {
  private ticketService = inject(TicketService);
  private customerService = inject(CustomerService);
  private session = inject(EmployeeSessionService);
  private fb = inject(FormBuilder);
  private router = inject(Router);

  tickets = signal<Ticket[]>([]);
  customers = signal<Customer[]>([]);
  dialogVisible = false;

  deviceTypeOptions = DEVICE_TYPE_OPTIONS;
  statusLabels = TICKET_STATUS_LABELS;
  statusSeverity = TICKET_STATUS_SEVERITY;

  form = this.fb.nonNullable.group({
    customerId: this.fb.control<number | null>(null, Validators.required),
    problemDescription: ['', [Validators.required, Validators.maxLength(500)]],
    device: this.fb.nonNullable.group({
      type: ['OTHER' as const, Validators.required],
      brand: [''],
      model: [''],
      identifier: ['']
    })
  });

  ngOnInit(): void {
    this.loadTickets();
    this.customerService.getCustomers().subscribe(list => this.customers.set(list));
  }

  loadTickets(): void {
    this.ticketService.getTickets().subscribe(list => this.tickets.set(list));
  }

  openNew(): void {
    this.form.reset({ customerId: null, problemDescription: '', device: { type: 'OTHER', brand: '', model: '', identifier: '' } });
    this.dialogVisible = true;
  }

  save(): void {
    if (this.form.invalid) return;
    const employee = this.session.current();
    const customerId = this.form.controls.customerId.value;
    if (!employee || customerId === null) return;

    const raw = this.form.getRawValue();
    this.ticketService.createTicket({
      customerId,
      problemDescription: raw.problemDescription,
      device: { ...raw.device, identifier: raw.device.identifier || null },
      createdByEmployeeId: employee.id
    }).subscribe(() => {
      this.dialogVisible = false;
      this.loadTickets();
    });
  }

  openDetail(ticket: Ticket): void {
    this.router.navigate(['/tickets', ticket.publicId]);
  }

  getStatusLabel(status: string): string {
    return TICKET_STATUS_LABELS[status as TicketStatus] ?? status;
  }

  getStatusSeverity(status: string): 'warn' | 'info' | 'success' | 'danger' {
    return TICKET_STATUS_SEVERITY[status as TicketStatus] ?? 'info';
  }
}
