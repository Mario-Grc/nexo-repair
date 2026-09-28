import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Employee, EmployeeRole, NewEmployee } from '../models/employee';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class EmployeeService {
  private http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/employees`;

  getEmployees(filters?: { role?: EmployeeRole; active?: boolean }): Observable<Employee[]> {
    let params = new HttpParams();
    if (filters?.role) params = params.set('role', filters.role);
    if (filters?.active !== undefined) params = params.set('active', String(filters.active));
    return this.http.get<Employee[]>(this.baseUrl, { params });
  }

  createEmployee(employee: NewEmployee): Observable<Employee> {
    return this.http.post<Employee>(this.baseUrl, employee);
  }

  updateRole(id: number, role: EmployeeRole): Observable<Employee> {
    return this.http.patch<Employee>(`${this.baseUrl}/${id}/role`, { role });
  }

  updateActive(id: number, active: boolean): Observable<Employee> {
    return this.http.patch<Employee>(`${this.baseUrl}/${id}/active`, { active });
  }

  resetPassword(id: number, newPassword: string): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/${id}/password`, { newPassword });
  }

  updateProfile(id: number, profile: { name: string; email: string }): Observable<Employee> {
    return this.http.patch<Employee>(`${this.baseUrl}/${id}/profile`, profile);
  }
}
