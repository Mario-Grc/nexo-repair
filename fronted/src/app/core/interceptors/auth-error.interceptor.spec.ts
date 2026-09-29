import '@angular/compiler';
import { HttpErrorResponse, HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { Injector, runInInjectionContext } from '@angular/core';
import { Router } from '@angular/router';
import { throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { AuthService } from '../services/auth.service';
import { authErrorInterceptor } from './auth-error.interceptor';

function runRequest(url: string, status: number, routerUrl: string) {
  const clearSession = vi.fn();
  const navigateByUrl = vi.fn();
  const injector = Injector.create({
    providers: [
      { provide: AuthService, useValue: { clearSession } },
      { provide: Router, useValue: { url: routerUrl, navigateByUrl } },
    ],
  });
  const req = new HttpRequest('GET', url);
  const next: HttpHandlerFn = () => throwError(() => new HttpErrorResponse({ status, statusText: 'Error' }));
  const err = runInInjectionContext(
    injector,
    () =>
      new Promise<HttpErrorResponse>(resolve => {
        authErrorInterceptor(req, next).subscribe({
          next: () => resolve(new HttpErrorResponse({})),
          error: (e: HttpErrorResponse) => resolve(e),
        });
      }),
  );
  return err.then(e => ({ err: e, clearSession, navigateByUrl }));
}

describe('authErrorInterceptor', () => {
  it('clears the session, goes to login and keeps the error on 401', async () => {
    const { err, clearSession, navigateByUrl } = await runRequest('/api/tickets', 401, '/tickets');
    expect(err.status).toBe(401);
    expect(clearSession).toHaveBeenCalledOnce();
    expect(navigateByUrl).toHaveBeenCalledWith('/login');
  });

  it('skips navigation on a failed login', async () => {
    const { err, clearSession, navigateByUrl } = await runRequest('/api/auth/login', 401, '/tickets');
    expect(err.status).toBe(401);
    expect(navigateByUrl).not.toHaveBeenCalled();
    expect(clearSession).not.toHaveBeenCalled();
  });

  it('skips navigation on a failed password change', async () => {
    const { err, navigateByUrl } = await runRequest('/api/auth/password', 401, '/tickets');
    expect(err.status).toBe(401);
    expect(navigateByUrl).not.toHaveBeenCalled();
  });

  it('skips navigation when already on the login page', async () => {
    const { err, navigateByUrl } = await runRequest('/api/tickets', 401, '/login');
    expect(err.status).toBe(401);
    expect(navigateByUrl).not.toHaveBeenCalled();
  });

  it('skips navigation on 403', async () => {
    const { err, clearSession, navigateByUrl } = await runRequest('/api/tickets', 403, '/tickets');
    expect(err.status).toBe(403);
    expect(clearSession).not.toHaveBeenCalled();
    expect(navigateByUrl).not.toHaveBeenCalled();
  });

  it('skips navigation on 500', async () => {
    const { err, clearSession, navigateByUrl } = await runRequest('/api/tickets', 500, '/tickets');
    expect(err.status).toBe(500);
    expect(clearSession).not.toHaveBeenCalled();
    expect(navigateByUrl).not.toHaveBeenCalled();
  });
});
