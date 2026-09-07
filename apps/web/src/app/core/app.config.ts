import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { apiErrorInterceptor } from './interceptors/api-error.interceptor';
import { mockTokenInterceptor } from './interceptors/mock-token.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // withComponentInputBinding: UI-miniloan-003 reads :id as a signal input rather than
    // subscribing to ActivatedRoute (FE-miniloan-021).
    provideRouter(routes, withComponentInputBinding()),
    // FE-miniloan-029: the token goes on first, then apiErrorInterceptor wraps the call — so a
    // request it reports as unreachable is the same request the API would have been sent.
    provideHttpClient(withInterceptors([mockTokenInterceptor, apiErrorInterceptor])),
  ],
};
