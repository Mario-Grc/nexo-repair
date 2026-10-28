import { HttpContext, HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MessageService } from 'primeng/api';
import { catchError, throwError } from 'rxjs';
import { errorMessage, readServerMessage } from './http-error-message';

export const SKIP_ERROR_TOAST = new HttpContextToken<boolean>(() => false);

// Shared context for requests whose caller shows its own inline error,
// so the global error toast stays silent to avoid a duplicated message.
export function silentContext(): HttpContext {
  return new HttpContext().set(SKIP_ERROR_TOAST, true);
}

export const httpErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const messages = inject(MessageService);
  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status !== 401 && !req.context.get(SKIP_ERROR_TOAST)) {
        messages.add({ severity: 'error', summary: errorMessage(err.status, readServerMessage(err.error)) });
      }
      return throwError(() => err);
    }),
  );
};
