import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { mockTokenInterceptor } from './interceptors/mock-token.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // withComponentInputBinding: UI-miniloan-003 reads :id as a signal input rather than
    // subscribing to ActivatedRoute (FE-miniloan-021).
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([mockTokenInterceptor])),
  ],
};
