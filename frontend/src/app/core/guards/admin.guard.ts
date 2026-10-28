import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const fallback = () => router.parseUrl('/tickets');
  const checkAdmin = () => (auth.current()?.role === 'ADMIN' ? true : fallback());

  // After a page reload the signal is empty even when the session cookie
  // is still valid, so restore the session first before checking the role.
  if (auth.current() !== null) return checkAdmin();

  return auth.restoreSession().pipe(
    map(() => checkAdmin()),
    catchError(() => of(fallback())),
  );
};
