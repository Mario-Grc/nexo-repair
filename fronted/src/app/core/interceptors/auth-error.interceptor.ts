import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

const EXPECTED_UNAUTHORIZED_URLS = ['/api/auth/login', '/api/auth/password'];

export const authErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  return next(req).pipe(
    catchError(err => {
      // 401s from login and password change are expected domain errors
      // (wrong credentials, not an expired session). Each screen shows its own
      // message instead of redirecting. Skip navigation when already on /login too.
      if (err.status === 401 && !isExpectedUnauthorized(req.url) && !router.url.startsWith('/login')) {
        router.navigateByUrl('/login');
      }
      return throwError(() => err);
    }),
  );
};

function isExpectedUnauthorized(url: string): boolean {
  return EXPECTED_UNAUTHORIZED_URLS.some(path => url.includes(path));
}
