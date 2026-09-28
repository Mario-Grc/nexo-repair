import { Component, OnInit, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, FormsModule, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { SelectModule } from 'primeng/select';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { EmployeeService } from '../../../core/services/employee.service';
import { AuthService } from '../../../core/services/auth.service';
import { Employee, EmployeeRole } from '../../../core/models/employee';
import { ResetPasswordDialog } from '../reset-password-dialog/reset-password-dialog';
import { EditEmployeeDialog } from '../edit-employee-dialog/edit-employee-dialog';

@Component({
  selector: 'app-employee-list',
  imports: [
    TableModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    PasswordModule,
    SelectModule,
    ToggleSwitchModule,
    ReactiveFormsModule,
    FormsModule,
    ResetPasswordDialog,
    EditEmployeeDialog,
  ],
  templateUrl: './employee-list.html',
  styleUrl: './employee-list.css',
})
export class EmployeeList implements OnInit {
  private employeeService = inject(EmployeeService);
  private fb = inject(FormBuilder);

  /** Current session, used to lock the row of the logged in employee. */
  private readonly auth = inject(AuthService);

  employees = signal<Employee[]>([]);
  dialogVisible = signal(false);
  resetDialogVisible = signal(false);
  editDialogVisible = signal(false);
  selectedEmployeeId = signal<number | null>(null);
  editingEmployee = signal<Employee | null>(null);
  listError = signal<string | null>(null);
  isCreating = signal(false);
  createError = signal<string | null>(null);

  roleOptions: { label: string; value: EmployeeRole }[] = [
    { label: 'Technician', value: 'TECHNICIAN' },
    { label: 'Reception', value: 'RECEPTION' },
    { label: 'Admin', value: 'ADMIN' },
  ];

  form = this.fb.nonNullable.group({
    name: ['', [Validators.required, noBlank]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    role: ['TECHNICIAN' as EmployeeRole, Validators.required],
  });

  ngOnInit(): void {
    this.loadEmployees();
  }

  loadEmployees(): void {
    this.employeeService.getEmployees().subscribe({
      next: list => {
        this.employees.set(list);
        this.listError.set(null);
      },
      error: () => this.listError.set('Could not load employees.'),
    });
  }

  isSelf(employee: Employee): boolean {
    return this.auth.current()?.id === employee.id;
  }

  // Inline edits apply at once. The list reloads on success and on error
  // so the table never shows a value the backend rejected.
  onRoleChange(employee: Employee, role: EmployeeRole): void {
    if (role === employee.role) return;
    this.listError.set(null);
    this.employeeService.updateRole(employee.id, role).subscribe({
      next: () => this.loadEmployees(),
      error: (error: HttpErrorResponse) => {
        this.listError.set(readErrorMessage(error, 'Could not update the employee role.'));
        this.loadEmployees();
      },
    });
  }

  onActiveChange(employee: Employee, active: boolean): void {
    if (active === employee.active) return;
    this.listError.set(null);
    this.employeeService.updateActive(employee.id, active).subscribe({
      next: () => this.loadEmployees(),
      error: (error: HttpErrorResponse) => {
        this.listError.set(readErrorMessage(error, 'Could not update the employee status.'));
        this.loadEmployees();
      },
    });
  }

  openNew(): void {
    this.createError.set(null);
    this.form.reset({ name: '', email: '', password: '', role: 'TECHNICIAN' });
    this.dialogVisible.set(true);
  }

  create(): void {
    if (this.form.invalid || this.isCreating()) return;
    const raw = this.form.getRawValue();

    this.isCreating.set(true);
    this.createError.set(null);
    this.employeeService
      .createEmployee({ name: raw.name.trim(), email: raw.email.trim(), password: raw.password, role: raw.role })
      .subscribe({
        next: () => {
          this.isCreating.set(false);
          this.dialogVisible.set(false);
          this.loadEmployees();
        },
        error: (error: HttpErrorResponse) => {
          this.isCreating.set(false);
          this.createError.set(readErrorMessage(error, 'Could not create the employee.'));
        },
      });
  }

  openReset(employee: Employee): void {
    this.selectedEmployeeId.set(employee.id);
    this.resetDialogVisible.set(true);
  }

  openEdit(employee: Employee): void {
    this.editingEmployee.set(employee);
    this.editDialogVisible.set(true);
  }
}

function noBlank(control: AbstractControl): ValidationErrors | null {
  return control.value?.trim() ? null : { blank: true };
}

// The backend answers with a plain string body, so its message
// (self modification, not found, email in use) beats our generic one.
function readErrorMessage(error: HttpErrorResponse, fallback: string): string {
  return typeof error.error === 'string' && error.error ? error.error : fallback;
}
