import { Component, OnInit, ViewEncapsulation, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
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
import { TableModule } from 'primeng/table';
import { MenuItem } from 'primeng/api';
import { TicketService } from '../../../core/services/ticket.service';
import { CustomerService } from '../../../core/services/customer.service';
import { AssignableTechnician, EmployeeService } from '../../../core/services/employee.service';
import { Ticket } from '../../../core/models/ticket';
import { TicketPart } from '../../../core/models/ticket-part';
import { TicketStatus, TICKET_STATUS_LABELS, TICKET_STATUS_SEVERITY } from '../../../core/models/ticket-status';
import { TimelineEntry } from '../../../core/models/timeline-entry';
import { Customer } from '../../../core/models/customer';
import { DEVICE_TYPE_LABELS } from '../../../core/models/device-type';
import { STEPPER_ORDER, getStepperIndex } from './stepper-index';
import { StatusChangeDialog } from './status-change-dialog';
import { AddPartDialog } from './add-part-dialog';

@Component({
  selector: 'app-ticket-detail',
  imports: [RouterLink, DatePipe, CurrencyPipe, ReactiveFormsModule, TagModule, ButtonModule, SplitButtonModule, StepsModule, CardModule, SelectModule, TextareaModule, MessageModule, TableModule, StatusChangeDialog, AddPartDialog],
  templateUrl: './ticket-detail.html',
  styleUrl: './ticket-detail.css',
  // Required to theme PrimeNG internals. All rules stay under .ticket-detail.
  encapsulation: ViewEncapsulation.None,
})
export class TicketDetail implements OnInit {
  private route = inject(ActivatedRoute);
  private ticketService = inject(TicketService);
  private customerService = inject(CustomerService);
  private employeeService = inject(EmployeeService);
  private fb = inject(FormBuilder);

  private publicId = '';

  ticket = signal<Ticket | null>(null);
  timeline = signal<TimelineEntry[]>([]);
  customer = signal<Customer | null>(null);
  technicians = signal<AssignableTechnician[]>([]);
  parts = signal<TicketPart[]>([]);
  partsTotal = signal<number | null>(null);
  pendingStatus = signal<TicketStatus | null>(null);
  statusDialogVisible = signal(false);
  partDialogVisible = signal(false);
  error = signal<string | null>(null);
  // The technician selection is staged and saved only when Save is clicked.
  assignDirty = signal(false);

  statusLabels = TICKET_STATUS_LABELS;
  statusSeverity = TICKET_STATUS_SEVERITY;
  deviceTypeLabels = DEVICE_TYPE_LABELS;

  stepperItems: MenuItem[] = STEPPER_ORDER.map(status => ({ label: TICKET_STATUS_LABELS[status] }));

  shortId = computed(() => (this.ticket()?.publicId ?? '').slice(0, 8).toUpperCase());
  isCancelled = computed(() => this.ticket()?.status === 'CANCELLED');
  activeIndex = computed(() => {
    const status = this.ticket()?.status;
    return status ? getStepperIndex(status) : 0;
  });
  primaryStatus = computed(() => this.ticket()?.allowedNextStatuses[0] ?? null);
  splitButtonItems = computed<MenuItem[]>(() =>
    (this.ticket()?.allowedNextStatuses ?? []).slice(1).map(status => ({
      label: TICKET_STATUS_LABELS[status],
      command: () => this.changeStatus(status),
    })),
  );
  technicianOptions = computed(() => [
    { name: 'Unassigned', id: null as number | null },
    ...this.technicians().map(t => ({ name: t.name, id: t.id as number | null })),
  ]);

  noteForm = this.fb.nonNullable.group({
    text: ['', [Validators.required, Validators.maxLength(500)]],
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
    this.loadParts();
    this.employeeService.getAssignableTechnicians().subscribe(list => this.technicians.set(list));
  }

  loadTicket(): void {
    this.ticketService.getTicket(this.publicId).subscribe({
      next: ticket => {
        this.ticket.set(ticket);
        this.assignControl.setValue(ticket.assignedEmployeeId ?? null, { emitEvent: false });
        this.assignDirty.set(false);
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

  loadParts(): void {
    this.ticketService.getParts(this.publicId).subscribe(response => {
      this.parts.set(response.items);
      this.partsTotal.set(response.total);
    });
  }

  changeStatus(status: TicketStatus): void {
    this.pendingStatus.set(status);
    this.statusDialogVisible.set(true);
  }

  onStatusConfirmed(note: string | null): void {
    const status = this.pendingStatus();
    this.pendingStatus.set(null);
    if (!status) return;
    this.doChangeStatus(status, note);
  }

  onStatusDialogClosed(): void {
    this.pendingStatus.set(null);
  }

  private doChangeStatus(status: TicketStatus, note: string | null): void {
    const ticket = this.ticket();
    if (!ticket) return;
    this.ticketService
      .changeStatus(this.publicId, { newStatus: status, note })
      .subscribe({
        next: updated => {
          this.ticket.set(updated);
          this.loadTimeline();
        },
        error: e => this.error.set(e.status === 400 ? 'A note is required for this status.' : 'Could not change the status.'),
      });
  }

  onPartSaved(): void {
    this.loadParts();
    this.loadTimeline();
  }

  removePart(partId: number): void {
    this.ticketService.removePart(this.publicId, partId).subscribe({
      next: () => {
        this.loadParts();
        this.loadTimeline();
      },
      error: e => this.error.set(e.status === 409 ? 'The ticket is closed.' : 'Could not remove the part.'),
    });
  }

  onAssignStaged(): void {
    const ticket = this.ticket();
    this.assignDirty.set(ticket != null && (ticket.assignedEmployeeId ?? null) !== (this.assignControl.value ?? null));
  }

  onAssign(): void {
    this.ticketService.assignEmployee(this.publicId, {
      employeeId: this.assignControl.value ?? null,
    }).subscribe(updated => {
      this.ticket.set(updated);
      this.assignControl.setValue(updated.assignedEmployeeId ?? null, { emitEvent: false });
      this.assignDirty.set(false);
      this.loadTimeline();
    });
  }

  addNote(): void {
    if (this.noteForm.invalid) return;
    const text = this.noteForm.controls.text.value.trim();
    if (!text) return;
    this.ticketService.addNote(this.publicId, { text }).subscribe(() => {
      this.noteForm.reset();
      this.loadTimeline();
    });
  }
}
