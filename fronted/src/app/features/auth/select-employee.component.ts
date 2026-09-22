import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { Employee } from '../../core/models/employee';
import { EmployeeService } from '../../core/services/employee.service';
import { EmployeeSessionService } from '../../core/services/employee-session.service';

@Component({
  selector: 'app-select-employee',
  standalone: true,
  imports: [SelectModule, ButtonModule, FormsModule],
  template: `
    <div class="select-employee-screen">
      <h1>Nexo</h1>
      <p>Select the employee who is using the application.</p>
      <p-select
        [options]="employees()"
        optionLabel="name"
        placeholder="Choose your name"
        [(ngModel)]="selected"
      />
      <p-button label="Enter" [disabled]="!selected" (onClick)="enter()" />
    </div>
  `,
  styles: [`
    .select-employee-screen {
      max-width: 320px;
      margin: 15vh auto;
      display: flex;
      flex-direction: column;
      gap: 1rem;
      text-align: center;
    }
  `]
})
export class SelectEmployeeComponent {
  private employeeService = inject(EmployeeService);
  private session = inject(EmployeeSessionService);
  private router = inject(Router);

  employees = toSignal(this.employeeService.getEmployees(), { initialValue: [] as Employee[] });
  selected: Employee | null = null;

  enter(): void {
    if (this.selected) {
      this.session.setCurrent(this.selected);
      this.router.navigateByUrl('/tickets');
    }
  }
}