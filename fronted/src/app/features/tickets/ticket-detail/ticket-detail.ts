import { Component, OnInit, ViewEncapsulation, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { SplitButtonModule } from 'primeng/splitbutton';
import { StepsModule } from 'primeng/steps';
import { CardModule } from 'primeng/card';
import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';
import { MessageModule } from 'primeng/message';
import { MenuItem } from 'primeng/api';
import { TicketService } from '../../../core/services/ticket.service';
import { CustomerService } from '../../../core/services/customer.service';
import { EmployeeService } from '../../../core/services/employee.service';
import { EmployeeSessionService } from '../../../core/services/employee-session.service';
import { Ticket } from '../../../core/models/ticket';
import { TicketStatus, TICKET_STATUS_LABELS, TICKET_STATUS_SEVERITY } from '../../../core/models/ticket-status';
import { TimelineEntry } from '../../../core/models/timeline-entry';
import { Customer } from '../../../core/models/customer';
import { Employee } from '../../../core/models/employee';
import { DEVICE_TYPE_LABELS } from '../../../core/models/device-type';

// The stepper is linear: parts are expected before work begins, while the backend
// state machine still allows moving between IN_PROGRESS and WAITING_FOR_PARTS.
const STEPPER_ORDER: TicketStatus[] = ['PENDING', 'WAITING_FOR_PARTS', 'IN_PROGRESS', 'COMPLETED', 'DELIVERED'];

@Component({
  selector: 'app-ticket-detail',
  imports: [RouterLink, DatePipe, ReactiveFormsModule, TagModule, ButtonModule, SplitButtonModule, StepsModule, CardModule, SelectModule, TextareaModule, MessageModule],
  templateUrl: './ticket-detail.html',
  styleUrl: './ticket-detail.css',
  // Required to theme PrimeNG internals; CSS rules are scoped under .ticket-detail.
  encapsulation: ViewEncapsulation.None,
})
export class TicketDetail implements OnInit {
  private route = inject(ActivatedRoute);
  private ticketService = inject(TicketService);
  private customerService = inject(CustomerService);
  private employeeService = inject(EmployeeService);
  private session = inject(EmployeeSessionService);
  private fb = inject(FormBuilder);

  private publicId = '';

  ticket = signal<Ticket | null>(null);
  timeline = signal<TimelineEntry[]>([]);
  customer = signal<Customer | null>(null);
  technicians = signal<Employee[]>([]);
  error = signal<string | null>(null);

  statusLabels = TICKET_STATUS_LABELS;
  statusSeverity = TICKET_STATUS_SEVERITY;
  deviceTypeLabels = DEVICE_TYPE_LABELS;

  stepperItems: MenuItem[] = STEPPER_ORDER.map(status => ({ label: TICKET_STATUS_LABELS[status] }));

  shortId = computed(() => (this.ticket()?.publicId ?? '').slice(0, 8).toUpperCase());
  isCancelled = computed(() => this.ticket()?.status === 'CANCELLED');
  activeIndex = computed(() => {
    const status = this.ticket()?.status;
    return status ? STEPPER_ORDER.indexOf(status) : 0;
  });
  primaryStatus = computed(() => this.ticket()?.allowedNextStatuses[0] ?? null);
  splitButtonItems = computed<MenuItem[]>(() =>
    (this.ticket()?.allowedNextStatuses ?? []).slice(1).map(status => ({
      label: TICKET_STATUS_LABELS[status],
      command: () => this.changeStatus(status),
    })),
  );
  technicianOptions = computed(() => [
    { label: 'Unassigned', value: null as number | null },
    ...this.technicians().map(t => ({ label: t.name, value: t.id as number | null })),
  ]);

  noteForm = this.fb.nonNullable.group({
    text: ['', Validators.required],
  });

  assignControl = this.fb.control<number | null>(null);

  ngOnInit(): void {
    const publicId = this.route.snapshot.paramMap.get('publicId');
    if (!publicId) {
      this.error.set('Ticket not found.');
      return;
    }
    this.publicId = publicId;
    this.loadTicket();
    this.loadTimeline();
    this.employeeService.getEmployees({ role: 'TECHNICIAN', active: true }).subscribe(list => this.technicians.set(list));
  }

  loadTicket(): void {
    this.ticketService.getTicket(this.publicId).subscribe({
      next: ticket => {
        this.ticket.set(ticket);
        this.assignControl.setValue(ticket.assignedEmployeeId ?? null, { emitEvent: false });
        this.loadCustomer(ticket.customerId);
      },
      error: () => this.error.set('Ticket not found.'),
    });
  }

  loadTimeline(): void {
    this.ticketService.getTimeline(this.publicId).subscribe(list => this.timeline.set(list));
  }

  loadCustomer(customerId: number): void {
    this.customerService.getCustomer(customerId).subscribe(c => this.customer.set(c));
  }

  changeStatus(status: TicketStatus): void {
    const employee = this.session.current();
    const ticket = this.ticket();
    if (!employee || !ticket) return;
    this.ticketService
      .changeStatus(this.publicId, { newStatus: status, changedByEmployeeId: employee.id, note: null })
      .subscribe(updated => {
        this.ticket.set(updated);
        this.loadTimeline();
      });
  }

  onAssign(): void {
    this.ticketService.assignEmployee(this.publicId, this.assignControl.value ?? null).subscribe(updated => {
      this.ticket.set(updated);
      this.assignControl.setValue(updated.assignedEmployeeId ?? null, { emitEvent: false });
    });
  }

  addNote(): void {
    if (this.noteForm.invalid) return;
    const employee = this.session.current();
    if (!employee) return;
    const text = this.noteForm.controls.text.value.trim();
    if (!text) return;
    this.ticketService.addNote(this.publicId, { authorEmployeeId: employee.id, text }).subscribe(() => {
      this.noteForm.reset();
      this.loadTimeline();
    });
  }
}
