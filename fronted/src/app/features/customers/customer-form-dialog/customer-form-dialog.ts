import { Component, effect, inject, input, model, output, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ButtonModule } from 'primeng/button';
import { CustomerService } from '../../../core/services/customer.service';
import { Customer, NewCustomer } from '../../../core/models/customer';

@Component({
  selector: 'app-customer-form-dialog',
  imports: [DialogModule, InputTextModule, TextareaModule, ButtonModule, ReactiveFormsModule],
  templateUrl: './customer-form-dialog.html',
})
export class CustomerFormDialog {
  private customerService = inject(CustomerService);
  private fb = inject(FormBuilder);

  /** Two-way bound: parent opens with `dialogVisible.set(true)` and binds explicitly. */
  visible = model(false);
  /** Customer to edit, or null to create a new one. */
  customer = input<Customer | null>(null);

  saved = output<Customer>();

  isSaving = signal(false);
  saveError = signal<string | null>(null);

  form = this.fb.nonNullable.group(
    {
      name: ['', Validators.required],
      email: [''],
      phone: [''],
      notes: [''],
    },
    { validators: requireEmailOrPhone },
  );

  constructor() {
    // Sync the form every time the dialog opens (not while typing:
    // neither signal changes while the user edits).
    effect(() => {
      const isOpen = this.visible();
      const current = this.customer();
      if (!isOpen) return;
      this.saveError.set(null);
      this.form.reset(
        current
          ? {
              name: current.name,
              email: current.email ?? '',
              phone: current.phone ?? '',
              notes: current.notes ?? '',
            }
          : { name: '', email: '', phone: '', notes: '' },
      );
    });
  }

  save(): void {
    if (this.form.invalid || this.isSaving()) return;
    const raw = this.form.getRawValue();
    // Empty strings become null so Bean Validation treats them as absent.
    const payload: NewCustomer = {
      name: raw.name.trim(),
      email: raw.email.trim() || null,
      phone: raw.phone.trim() || null,
      notes: raw.notes.trim() || null,
    };
    const current = this.customer();

    this.isSaving.set(true);
    this.saveError.set(null);
    const request =
      current != null
        ? this.customerService.updateCustomer(current.id, payload)
        : this.customerService.createCustomer(payload);
    request.subscribe({
      next: result => {
        this.isSaving.set(false);
        this.saved.emit(result);
        this.visible.set(false);
      },
      error: () => {
        this.isSaving.set(false);
        this.saveError.set(current != null ? 'Could not update the customer.' : 'Could not create the customer.');
      },
    });
  }
}

export function requireEmailOrPhone(group: AbstractControl): ValidationErrors | null {
  const email = group.get('email')?.value?.trim();
  const phone = group.get('phone')?.value?.trim();
  return email || phone ? null : { contactRequired: true };
}
