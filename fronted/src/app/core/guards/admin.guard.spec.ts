import '@angular/compiler';
import { Injector, runInInjectionContext, signal } from '@angular/core';
import { ActivatedRouteSnapshot, DefaultUrlSerializer, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { firstValueFrom, isObservable, Observable, of, tap, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { Employee, EmployeeRole } from '../models/employee';
import { AuthService } from '../services/auth.service';
import { adminGuard } from './admin.guard';

function user(role: EmployeeRole): Employee {
  return { id: 1, name: 'Ada', email: 'ada@nexo.com', role, active: true };
}

async function runGuard(current: Employee | null, restore?: () => Observable<Employee>) {
  const currentSignal = signal(current);
  const injector = Injector.create({
    providers: [
      {
        provide: AuthService,
        useValue: {
          current: currentSignal.asReadonly(),
          // Mimic the real AuthService: restoreSession() sets the signal via tap.
          restoreSession: vi.fn().mockImplementation(() => {
            const source = restore?.() ?? of(user('ADMIN'));
            return source.pipe(tap(employee => currentSignal.set(employee)));
          }),
        },
      },
      { provide: Router, useValue: { parseUrl: (url: string) => new DefaultUrlSerializer().parse(url) } },
    ],
  });
  const raw = runInInjectionContext(injector, () =>
    adminGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
  );
  return isObservable(raw) ? firstValueFrom(raw) : raw;
}

describe('adminGuard', () => {
  it('lets an admin in', async () => {
    expect(await runGuard(user('ADMIN'))).toBe(true);
  });

  it('sends a technician to the ticket list', async () => {
    const result = await runGuard(user('TECHNICIAN'));
    expect(result instanceof UrlTree).toBe(true);
    expect((result as UrlTree).toString()).toBe('/tickets');
  });

  it('sends reception to the ticket list', async () => {
    const result = await runGuard(user('RECEPTION'));
    expect(result instanceof UrlTree).toBe(true);
    expect((result as UrlTree).toString()).toBe('/tickets');
  });

  it('restores an admin session after a reload', async () => {
    const result = await runGuard(null, () => of(user('ADMIN')));
    expect(result).toBe(true);
  });

  it('sends a failed restore to the ticket list', async () => {
    const result = await runGuard(null, () => throwError(() => new Error('expired')));
    expect(result instanceof UrlTree).toBe(true);
    expect((result as UrlTree).toString()).toBe('/tickets');
  });
});
