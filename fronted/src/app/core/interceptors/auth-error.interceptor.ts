import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const authErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  return next(req).pipe(
    catchError(err => {
      // A failed login does not redirect. The component shows the error instead.
      // Skip navigation when already on /login (for example the initial restoreSession).
      if (err.status === 401 && !req.url.includes('/api/auth/login') && !router.url.startsWith('/login')) {
        router.navigateByUrl('/login');
      }
      return throwError(() => err);
    }),
  );
};
