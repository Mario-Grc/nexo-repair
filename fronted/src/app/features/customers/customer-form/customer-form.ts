import { Component, inject, output, signal } from '@angular/core';
import { CustomerService } from '../customer.service';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  selector: 'app-customer-form',
  imports: [ReactiveFormsModule],
  templateUrl: './customer-form.html',
  styleUrl: './customer-form.css',
})
export class CustomerForm {
  private customerService = inject(CustomerService);
  private fb = inject(NonNullableFormBuilder);

  created = output<void>();

  isSubmitting = signal(false);

  form = this.fb.group({
    name: ['', Validators.required],
    contact: ['', Validators.required],
  })

  onSubmit() {
    if (this.form.invalid || this.isSubmitting()) return;

    this.isSubmitting.set(true);

    this.customerService.createCustomer(this.form.getRawValue()).subscribe({
      next: () => {
        this.form.reset();
        this.created.emit();
        this.isSubmitting.set(false);
      },
      error: () => {
        console.error('Error creating customer');
        this.isSubmitting.set(false);
      }
    });
  }
}
