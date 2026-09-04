import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';

import { CurrentRoleService } from '../services/current-role.service';

/**
 * Attaches the mock token on every outgoing API request per BR-miniloan-030@v1
 * ("Web เรียก API ต้องแนบ token จำลองทุกครั้ง"). Scheme confirmed with the
 * project owner: standard `Authorization: Bearer <token>`, no login flow —
 * the token is the fixed per-role demo constant from CurrentRoleService.
 */
export const mockTokenInterceptor: HttpInterceptorFn = (req, next) => {
  const currentRole = inject(CurrentRoleService);
  const token = currentRole.role().mockToken;

  return next(
    req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    }),
  );
};
