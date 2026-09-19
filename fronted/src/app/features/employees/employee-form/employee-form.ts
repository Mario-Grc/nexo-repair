import { Component, inject, output, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { EmployeeService } from '../employee.service';

@Component({
  selector: 'app-employee-form',
  imports: [ReactiveFormsModule],
  templateUrl: './employee-form.html',
})
export class EmployeeForm {
  private employeeService = inject(EmployeeService);
  private fb = inject(NonNullableFormBuilder);

  created = output<void>();
  isSubmitting = signal(false);

  form = this.fb.group({
    name: ['', Validators.required],
  });

  onSubmit() {
    if (this.form.invalid || this.isSubmitting()) return;

    this.isSubmitting.set(true);
    this.employeeService.createEmployee(this.form.getRawValue()).subscribe({
      next: () => {
        this.form.reset();
        this.created.emit();
        this.isSubmitting.set(false);
      },
      error: () => this.isSubmitting.set(false),
    });
  }
}
