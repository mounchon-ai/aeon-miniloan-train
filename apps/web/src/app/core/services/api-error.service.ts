import { Injectable, signal } from '@angular/core';

/**
 * The one piece of state {@code apiErrorInterceptor} and the shell's error banner share
 * (UC-miniloan-026 · BR-miniloan-028@v1 · AC-miniloan-119 · AC-miniloan-120).
 *
 * <p>A service rather than a module-level signal inside the interceptor file: the interceptor is a
 * function that runs once per request, and mutable state parked beside it would be shared across
 * every TestBed in the suite with nothing to reset it.
 *
 * <p><b>It holds a connectivity message and nothing else.</b> An API that answers with 403 or 409 is
 * working — its sentence belongs to the screen that made the call, and ten screens already render
 * those. What this holds is the case where there is no answer at all.
 */
@Injectable({ providedIn: 'root' })
export class ApiErrorService {
  private readonly current = signal<string | null>(null);

  /** Null whenever the API is answering — which is what makes AC-miniloan-120 self-clearing. */
  readonly message = this.current.asReadonly();

  raise(message: string): void {
    this.current.set(message);
  }

  /**
   * AC-miniloan-120: "สถานะข้อผิดพลาดหายไปเอง". Any answer at all clears it, a refusal included —
   * a 403 means the API is up, and leaving the "cannot connect" banner on screen beside it would be
   * two contradictory statements about the same system.
   */
  clear(): void {
    this.current.set(null);
  }
}
