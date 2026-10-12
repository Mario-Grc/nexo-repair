import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, NgTemplateOutlet } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, debounceTime, distinctUntilChanged, filter, finalize, of, switchMap, tap } from 'rxjs';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DrawerModule } from 'primeng/drawer';
import { PaginatorModule } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';
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
import { ViewportService } from '../../../core/services/viewport.service';
import { Customer } from '../../../core/models/customer';
import { Ticket } from '../../../core/models/ticket';
import { DEVICE_TYPE_OPTIONS } from '../../../core/models/device-type';
import { TicketStatus, TICKET_STATUS_LABELS, TICKET_STATUS_SEVERITY, ALL_TICKET_STATUSES } from '../../../core/models/ticket-status';
import {
  defaultFilters,
  filtersFromParams,
  paramsFromFilters,
  toIsoDate,
  type TicketFilters,
} from './ticket-filters';

@Component({
  selector: 'app-ticket-list',
  imports: [
    TableModule,
    ButtonModule,
    DialogModule,
    DrawerModule,
    PaginatorModule,
    SkeletonModule,
    InputTextModule,
    TextareaModule,
    SelectModule,
    MultiSelectModule,
    DatePickerModule,
    TagModule,
    ReactiveFormsModule,
    FormsModule,
    DatePipe,
    RouterLink,
    NgTemplateOutlet,
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
  viewport = inject(ViewportService);

  filters = signal<TicketFilters>(defaultFilters(null, null));
  tickets = signal<Ticket[]>([]);
  totalElements = signal(0);
  loading = signal(false);
  loadError = signal<string | null>(null);
  hasLoaded = signal(false);
  isSaving = signal(false);
  technicians = signal<AssignableTechnician[]>([]);
  customers = signal<Customer[]>([]);
  dialogVisible = signal(false);
  filtersDrawerVisible = signal(false);

  searchControl = new FormControl('', { nonNullable: true });

  statusOptions = ALL_TICKET_STATUSES.map(status => ({ label: TICKET_STATUS_LABELS[status], value: status }));

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

  // Counts only filters that differ from the default view, so entering
  // with defaults shows Filters (0) and the "no tickets yet" empty state.
  activeFilterCount = computed(() => {
    const current = this.filters();
    const me = this.auth.current();
    const defaults = defaultFilters(me?.role ?? null, me?.id ?? null);
    let count = 0;
    if (!sameStatusSet(current.status, defaults.status)) count += 1;
    if (current.assignee !== defaults.assignee) count += 1;
    if (current.createdFrom !== null || current.createdTo !== null) count += 1;
    if (current.q.trim() !== '') count += 1;
    return count;
  });

  isFiltered = computed(() => this.activeFilterCount() > 0);

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
          this.loadError.set(null);
          return this.ticketService.getTickets(next).pipe(
            finalize(() => this.loading.set(false)),
            catchError(() => {
              this.loadError.set('Could not load tickets.');
              this.hasLoaded.set(true);
              return of({ content: [], page: next.page, size: next.size, totalElements: 0, totalPages: 0 });
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(page => {
        this.tickets.set(page.content);
        this.totalElements.set(page.totalElements);
        this.hasLoaded.set(true);
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

  // p-table emits numbers while p-paginator fields are optional,
  // so missing values fall back to the current page and size.
  onPage(event: { first?: number; rows?: number }): void {
    const current = this.filters();
    const rows = event.rows ?? current.size;
    const page = Math.floor((event.first ?? current.page * current.size) / rows);
    if (page === current.page && rows === current.size) return;
    this.navigateWith({ page, size: rows });
  }

  retry(): void {
    const current = this.filters();
    this.loading.set(true);
    this.loadError.set(null);
    this.ticketService.getTickets(current).subscribe({
      next: page => {
        this.tickets.set(page.content);
        this.totalElements.set(page.totalElements);
        this.loading.set(false);
        this.hasLoaded.set(true);
      },
      error: () => {
        this.tickets.set([]);
        this.totalElements.set(0);
        this.loading.set(false);
        this.hasLoaded.set(true);
        this.loadError.set('Could not load tickets.');
      },
    });
  }

  resetFilters(): void {
    this.filtersDrawerVisible.set(false);
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
    if (this.form.invalid || this.isSaving()) return;
    const customerId = this.form.controls.customerId.value;
    if (customerId === null) return;

    const raw = this.form.getRawValue();
    this.isSaving.set(true);
    this.ticketService
      .createTicket({
        customerId,
        problemDescription: raw.problemDescription,
        device: { ...raw.device, identifier: raw.device.identifier || null },
      })
      .subscribe({
        next: () => {
          this.isSaving.set(false);
          this.dialogVisible.set(false);
          this.retry();
        },
        error: () => this.isSaving.set(false),
      });
  }

  shortId(publicId: string): string {
    return publicId.slice(0, 8).toUpperCase();
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
}

// Same values regardless of order, so a manual selection equal to the
// default view still counts as unfiltered.
function sameStatusSet(current: TicketStatus[], defaults: TicketStatus[]): boolean {
  return current.length === defaults.length && current.every(status => defaults.includes(status));
}

// The URL stores dates without time, so parsing builds a local midnight.
function parseIsoDate(value: string): Date {
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}
