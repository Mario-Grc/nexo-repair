import { Component, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { EmployeeForm } from '../employee-form/employee-form';
import { EmployeeService } from '../employee.service';

@Component({
  selector: 'app-employee-list',
  imports: [EmployeeForm],
  templateUrl: './employee-list.html',
})
export class EmployeeList {
  private employeeService = inject(EmployeeService);

  employeesResource = rxResource({
    stream: () => this.employeeService.getEmployees(),
  });
}
