import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Employee, EmployeeRole } from '../models/employee';
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
}