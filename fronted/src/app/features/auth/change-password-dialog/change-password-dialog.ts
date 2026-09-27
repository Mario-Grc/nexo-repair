import { Component, inject, model, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { DialogModule } from 'primeng/dialog';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-change-password-dialog',
  imports: [DialogModule, PasswordModule, ButtonModule, ReactiveFormsModule],
  templateUrl: './change-password-dialog.html',
})
export class ChangePasswordDialog {
  private authService = inject(AuthService);
  private fb = inject(FormBuilder);

  /** Two-way bound: parent opens with `passwordDialogVisible.set(true)` and binds explicitly. */
  visible = model(false);

  isSaving = signal(false);
  saveError = signal<string | null>(null);

  form = this.fb.nonNullable.group(
    {
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required],
    },
    { validators: passwordsMatch },
  );

  // PrimeNG fires onShow every time the dialog opens. Plain method, no signals
  // written inside an effect, so no change detection surprises.
  onOpen(): void {
    this.saveError.set(null);
    this.form.reset({ currentPassword: '', newPassword: '', confirmPassword: '' });
  }

  save(): void {
    if (this.form.invalid || this.isSaving()) return;
    const raw = this.form.getRawValue();

    this.isSaving.set(true);
    this.saveError.set(null);
    this.authService.changePassword(raw.currentPassword, raw.newPassword).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.visible.set(false);
        this.form.reset({ currentPassword: '', newPassword: '', confirmPassword: '' });
      },
      error: (error: HttpErrorResponse) => {
        this.isSaving.set(false);
        // Wrong current password is an expected case with its own message.
        // Any other failure gets a generic message.
        this.saveError.set(error.status === 401 ? 'Current password is incorrect.' : 'Could not change the password.');
      },
    });
  }
}

export function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const next = group.get('newPassword')?.value;
  const confirm = group.get('confirmPassword')?.value;
  return next && confirm && next !== confirm ? { passwordsMismatch: true } : null;
}
