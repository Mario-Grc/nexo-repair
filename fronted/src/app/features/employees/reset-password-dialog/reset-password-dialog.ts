import { Component, inject, input, model, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { DialogModule } from 'primeng/dialog';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { EmployeeService } from '../../../core/services/employee.service';

@Component({
  selector: 'app-reset-password-dialog',
  imports: [DialogModule, PasswordModule, ButtonModule, ReactiveFormsModule],
  templateUrl: './reset-password-dialog.html',
})
export class ResetPasswordDialog {
  private employeeService = inject(EmployeeService);
  private fb = inject(FormBuilder);

  /** Two-way bound: parent opens with `resetDialogVisible.set(true)` and binds explicitly. */
  visible = model(false);
  /** Employee whose password will be reset, or null when no row is selected. */
  employeeId = input<number | null>(null);

  isSaving = signal(false);
  saveError = signal<string | null>(null);

  form = this.fb.nonNullable.group(
    {
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required],
    },
    { validators: passwordsMatch },
  );

  // PrimeNG fires onShow every time the dialog opens. Plain method, no signals
  // written inside an effect, so no change detection surprises.
  onOpen(): void {
    this.saveError.set(null);
    this.form.reset({ newPassword: '', confirmPassword: '' });
  }

  save(): void {
    const id = this.employeeId();
    if (this.form.invalid || this.isSaving() || id == null) return;
    const raw = this.form.getRawValue();

    this.isSaving.set(true);
    this.saveError.set(null);
    this.employeeService.resetPassword(id, raw.newPassword).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.visible.set(false);
        this.form.reset({ newPassword: '', confirmPassword: '' });
      },
      error: (error: HttpErrorResponse) => {
        this.isSaving.set(false);
        // Forbidden means a non admin tried to reset a password.
        // Other backend answers already carry a clear message, reuse it.
        this.saveError.set(
          error.status === 403
            ? 'You do not have permission for this action.'
            : readErrorMessage(error, 'Could not reset the password.'),
        );
      },
    });
  }
}

function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const next = group.get('newPassword')?.value;
  const confirm = group.get('confirmPassword')?.value;
  return next && confirm && next !== confirm ? { passwordsMismatch: true } : null;
}

// The backend answers with a plain string body, so its message beats our generic one.
function readErrorMessage(error: HttpErrorResponse, fallback: string): string {
  return typeof error.error === 'string' && error.error ? error.error : fallback;
}
