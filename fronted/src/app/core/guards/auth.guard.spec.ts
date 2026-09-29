import '@angular/compiler';
import { Injector, runInInjectionContext } from '@angular/core';
import { ActivatedRouteSnapshot, DefaultUrlSerializer, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { firstValueFrom, isObservable, of, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { Employee } from '../models/employee';
import { AuthService } from '../services/auth.service';
import { authGuard } from './auth.guard';

function runGuard(authValue: unknown) {
  const injector = Injector.create({
    providers: [
      { provide: AuthService, useValue: authValue },
      { provide: Router, useValue: { parseUrl: (url: string) => new DefaultUrlSerializer().parse(url) } },
    ],
  });
  const raw = runInInjectionContext(injector, () =>
    authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
  );
  return isObservable(raw) ? firstValueFrom(raw) : raw;
}

describe('authGuard', () => {
  it('lets an authenticated user in without restoring the session', async () => {
    const restoreSession = vi.fn();
    const result = await runGuard({ isLoggedIn: true, restoreSession });
    expect(result).toBe(true);
    expect(restoreSession).not.toHaveBeenCalled();
  });

  it('restores the session when the user is not logged in', async () => {
    const restoreSession = vi.fn().mockReturnValue(of({} as Employee));
    const result = await runGuard({ isLoggedIn: false, restoreSession });
    expect(result).toBe(true);
    expect(restoreSession).toHaveBeenCalledOnce();
  });

  it('sends a failed restore to the login page', async () => {
    const restoreSession = vi.fn().mockReturnValue(throwError(() => new Error('expired')));
    const result = await runGuard({ isLoggedIn: false, restoreSession });
    expect(result instanceof UrlTree).toBe(true);
    expect((result as UrlTree).toString()).toBe('/login');
  });
});
