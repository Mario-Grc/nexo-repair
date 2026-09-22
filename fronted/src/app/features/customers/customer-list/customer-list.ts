import { Component, OnInit, inject, signal } from '@angular/core';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { CustomerService } from '../../../core/services/customer.service';
import { Customer } from '../../../core/models/customer';

@Component({
  selector: 'app-customer-list',
  imports: [TableModule, ButtonModule, DialogModule, InputTextModule, TextareaModule, ReactiveFormsModule],
  templateUrl: './customer-list.html',
  styleUrl: './customer-list.css',
})
export class CustomerList implements OnInit {
  private customerService = inject(CustomerService);
  private fb = inject(FormBuilder);

  customers = signal<Customer[]>([]);
  dialogVisible = false;

  form = this.fb.nonNullable.group(
    {
      name: ['', Validators.required],
      email: [''],
      phone: [''],
      notes: [''],
    },
    { validators: requireEmailOrPhone },
  );

  ngOnInit(): void {
    this.loadCustomers();
  }

  loadCustomers(): void {
    this.customerService.getCustomers().subscribe(list => this.customers.set(list));
  }

  openNew(): void {
    this.form.reset();
    this.dialogVisible = true;
  }

  save(): void {
    if (this.form.invalid) return;
    this.customerService.createCustomer(this.form.getRawValue()).subscribe(() => {
      this.dialogVisible = false;
      this.loadCustomers();
    });
  }
}

function requireEmailOrPhone(group: AbstractControl): ValidationErrors | null {
  const email = group.get('email')?.value?.trim();
  const phone = group.get('phone')?.value?.trim();
  return email || phone ? null : { contactRequired: true };
}
