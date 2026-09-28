import { Component, inject, input, model, output, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { EmployeeService } from '../../../core/services/employee.service';
import { Employee } from '../../../core/models/employee';

@Component({
  selector: 'app-edit-employee-dialog',
  imports: [DialogModule, InputTextModule, ButtonModule, ReactiveFormsModule],
  templateUrl: './edit-employee-dialog.html',
})
export class EditEmployeeDialog {
  private employeeService = inject(EmployeeService);
  private fb = inject(FormBuilder);

  /** Two-way bound: parent opens with `editDialogVisible.set(true)` and binds explicitly. */
  visible = model(false);
  /** Employee being edited, or null when no row is selected. */
  employee = input<Employee | null>(null);
  /** Emits the updated employee so the list can reload. */
  saved = output<Employee>();

  isSaving = signal(false);
  saveError = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    name: ['', [Validators.required, noBlank]],
    email: ['', [Validators.required, Validators.email]],
  });

  // PrimeNG fires onShow every time the dialog opens. Plain method, no signals
  // written inside an effect, so no change detection surprises.
  onOpen(): void {
    this.saveError.set(null);
    this.form.reset({ name: this.employee()?.name ?? '', email: this.employee()?.email ?? '' });
  }

  save(): void {
    const employee = this.employee();
    if (this.form.invalid || this.isSaving() || employee == null) return;
    const raw = this.form.getRawValue();

    this.isSaving.set(true);
    this.saveError.set(null);
    this.employeeService
      .updateProfile(employee.id, { name: raw.name.trim(), email: raw.email.trim() })
      .subscribe({
        next: updated => {
          this.isSaving.set(false);
          this.visible.set(false);
          this.saved.emit(updated);
        },
        error: (error: HttpErrorResponse) => {
          this.isSaving.set(false);
          this.saveError.set(readErrorMessage(error, 'Could not update the employee.'));
        },
      });
  }
}

function noBlank(control: AbstractControl): ValidationErrors | null {
  return control.value?.trim() ? null : { blank: true };
}

// The backend answers with a plain string body, so its message
// (email in use, not found) beats our generic one.
function readErrorMessage(error: HttpErrorResponse, fallback: string): string {
  return typeof error.error === 'string' && error.error ? error.error : fallback;
}
