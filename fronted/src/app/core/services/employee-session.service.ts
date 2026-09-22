import { Injectable, signal } from '@angular/core';
import { Employee } from '../models/employee';

@Injectable({ providedIn: 'root' })
export class EmployeeSessionService {
  private readonly _current = signal<Employee | null>(null);
  readonly current = this._current.asReadonly();

  setCurrent(employee: Employee): void {
    this._current.set(employee);
  }

  clear(): void {
    this._current.set(null);
  }

  get isLoggedIn(): boolean {
    return this._current() !== null;
  }
}