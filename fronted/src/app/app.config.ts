import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { providePrimeNG } from 'primeng/config';
import { NexoPreset } from './theme/nexo-preset';
import { credentialsInterceptor } from './core/interceptors/credentials.interceptor';
import { authErrorInterceptor } from './core/interceptors/auth-error.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([credentialsInterceptor, authErrorInterceptor])),
    providePrimeNG({
      theme: {
        preset: NexoPreset,
        options: {
          darkModeSelector: false
        }
      }
    })
  ]
};
