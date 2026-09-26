import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { tap } from 'rxjs';
import { Employee } from '../models/employee';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/auth`;

  private readonly _current = signal<Employee | null>(null);
  readonly current = this._current.asReadonly();

  login(email: string, password: string) {
    return this.http
      .post<Employee>(`${this.baseUrl}/login`, { email, password })
      .pipe(tap(employee => this._current.set(employee)));
  }

  logout() {
    return this.http.post(`${this.baseUrl}/logout`, {}).pipe(tap(() => this._current.set(null)));
  }

  restoreSession() {
    return this.http.get<Employee>(`${this.baseUrl}/me`).pipe(tap(employee => this._current.set(employee)));
  }

  get isLoggedIn(): boolean {
    return this._current() !== null;
  }
}
