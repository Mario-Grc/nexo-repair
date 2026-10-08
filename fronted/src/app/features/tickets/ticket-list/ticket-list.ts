import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, debounceTime, distinctUntilChanged, filter, finalize, of, switchMap, tap } from 'rxjs';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { MultiSelectModule } from 'primeng/multiselect';
import { DatePickerModule } from 'primeng/datepicker';
import { TagModule } from 'primeng/tag';
import { TicketService } from '../../../core/services/ticket.service';
import { CustomerService } from '../../../core/services/customer.service';
import { EmployeeService, type AssignableTechnician } from '../../../core/services/employee.service';
import { AuthService } from '../../../core/services/auth.service';
import { Customer } from '../../../core/models/customer';
import { Ticket } from '../../../core/models/ticket';
import { DEVICE_TYPE_OPTIONS } from '../../../core/models/device-type';
import { TicketStatus, TICKET_STATUS_LABELS, TICKET_STATUS_SEVERITY } from '../../../core/models/ticket-status';
import {
  defaultFilters,
  filtersFromParams,
  paramsFromFilters,
  toIsoDate,
  type TicketFilters,
} from './ticket-filters';

const ALL_STATUSES: TicketStatus[] = [
  'PENDING',
  'IN_PROGRESS',
  'WAITING_FOR_PARTS',
  'COMPLETED',
  'DELIVERED',
  'CANCELLED',
];

@Component({
  selector: 'app-ticket-list',
  imports: [
    TableModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    TextareaModule,
    SelectModule,
    MultiSelectModule,
    DatePickerModule,
    TagModule,
    ReactiveFormsModule,
    FormsModule,
    DatePipe,
  ],
  templateUrl: './ticket-list.html',
  styleUrl: './ticket-list.css',
})
export class TicketList implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  private ticketService = inject(TicketService);
  private customerService = inject(CustomerService);
  private employeeService = inject(EmployeeService);
  private auth = inject(AuthService);
  private fb = inject(FormBuilder);

  filters = signal<TicketFilters>(defaultFilters(null, null));
  tickets = signal<Ticket[]>([]);
  totalElements = signal(0);
  loading = signal(false);
  technicians = signal<AssignableTechnician[]>([]);
  customers = signal<Customer[]>([]);
  dialogVisible = signal(false);

  searchControl = new FormControl('', { nonNullable: true });

  statusOptions = ALL_STATUSES.map(status => ({ label: TICKET_STATUS_LABELS[status], value: status }));

  assigneeOptions = computed(() => [
    { label: 'All technicians', value: null as number | 'unassigned' | null },
    { label: 'Unassigned', value: 'unassigned' as number | 'unassigned' | null },
    ...this.technicians().map(t => ({ label: t.name, value: t.id as number | 'unassigned' | null })),
  ]);

  // Date picker works with Dates while the URL keeps ISO strings.
  dateRange = computed<Date[] | null>(() => {
    const current = this.filters();
    const from = current.createdFrom === null ? null : parseIsoDate(current.createdFrom);
    const to = current.createdTo === null ? null : parseIsoDate(current.createdTo);
    if (from !== null && to !== null) return [from, to];
    if (from !== null) return [from];
    if (to !== null) return [to];
    return null;
  });

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
      identifier: [''],
    }),
  });

  ngOnInit(): void {
    this.employeeService
      .getAssignableTechnicians()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(list => this.technicians.set(list));
    this.customerService
      .getCustomers()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(list => this.customers.set(list));

    this.searchControl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(q => {
        if (q !== this.filters().q) this.navigateWith({ q: q ?? '', page: 0 });
      });

    this.route.queryParamMap
      .pipe(
        tap(paramMap => {
          if (paramMap.keys.length === 0) {
            const me = this.auth.current();
            const defaults = defaultFilters(me?.role ?? null, me?.id ?? null);
            this.router.navigate([], {
              relativeTo: this.route,
              queryParams: paramsFromFilters(defaults),
              replaceUrl: true,
            });
          }
        }),
        filter(paramMap => paramMap.keys.length > 0),
        tap(paramMap => {
          const next = filtersFromParams(paramMap);
          this.filters.set(next);
          if (this.searchControl.value !== next.q) this.searchControl.setValue(next.q, { emitEvent: false });
        }),
        switchMap(paramMap => {
          const next = filtersFromParams(paramMap);
          this.loading.set(true);
          return this.ticketService.getTickets(next).pipe(
            finalize(() => this.loading.set(false)),
            catchError(() =>
              of({ content: [], page: next.page, size: next.size, totalElements: 0, totalPages: 0 }),
            ),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(page => {
        this.tickets.set(page.content);
        this.totalElements.set(page.totalElements);
      });
  }

  onStatusChange(value: TicketStatus[] | null): void {
    this.navigateWith({ status: value ?? [], page: 0 });
  }

  onAssigneeChange(value: number | 'unassigned' | null): void {
    this.navigateWith({ assignee: value, page: 0 });
  }

  onDateRangeChange(value: Date[] | Date | null): void {
    const dates = value instanceof Date ? [value] : (value ?? []);
    if (dates.length === 0) {
      this.navigateWith({ createdFrom: null, createdTo: null, page: 0 });
      return;
    }
    const from = dates[0] instanceof Date ? toIsoDate(dates[0]) : null;
    const second = dates.length > 1 ? dates[1] : null;
    const to = second instanceof Date ? toIsoDate(second) : null;
    const current = this.filters();
    if (current.createdFrom === from && current.createdTo === to) return;
    this.navigateWith({ createdFrom: from, createdTo: to, page: 0 });
  }

  onPage(event: { first: number; rows: number }): void {
    const page = Math.floor(event.first / event.rows);
    const current = this.filters();
    if (page === current.page && event.rows === current.size) return;
    this.navigateWith({ page, size: event.rows });
  }

  resetFilters(): void {
    this.router.navigate(['/tickets']);
  }

  openNew(): void {
    this.form.reset({
      customerId: null,
      problemDescription: '',
      device: { type: 'OTHER', brand: '', model: '', identifier: '' },
    });
    this.dialogVisible.set(true);
  }

  save(): void {
    if (this.form.invalid) return;
    const customerId = this.form.controls.customerId.value;
    if (customerId === null) return;

    const raw = this.form.getRawValue();
    this.ticketService
      .createTicket({
        customerId,
        problemDescription: raw.problemDescription,
        device: { ...raw.device, identifier: raw.device.identifier || null },
      })
      .subscribe(() => {
        this.dialogVisible.set(false);
        this.refresh();
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

  private navigateWith(patch: Partial<TicketFilters>): void {
    const next: TicketFilters = { ...this.filters(), ...patch };
    this.router.navigate([], { relativeTo: this.route, queryParams: paramsFromFilters(next) });
  }

  // Reload the current page after a mutation. Filter changes navigate
  // instead, so this stays out of the URL driven flow.
  private refresh(): void {
    const current = this.filters();
    this.loading.set(true);
    this.ticketService.getTickets(current).subscribe({
      next: page => {
        this.tickets.set(page.content);
        this.totalElements.set(page.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.tickets.set([]);
        this.totalElements.set(0);
        this.loading.set(false);
      },
    });
  }
}

// The URL stores dates without time, so parsing builds a local midnight.
function parseIsoDate(value: string): Date {
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}
