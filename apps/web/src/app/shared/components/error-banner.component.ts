import { Component, input } from '@angular/core';

/**
 * The connectivity banner (UC-miniloan-026 · AC-miniloan-119 · AC-miniloan-120).
 *
 * <p><b>It renders a message and decides nothing.</b> Everything it knows arrives as an input, which
 * is what keeps it in `shared`: it injects nothing, reads no service, and issues no request. The
 * shell owns the state and passes it down.
 *
 * <p><b>It sits BESIDE the page, never in place of it.</b> AC-miniloan-119 says so twice —
 * "ไม่ใช่หน้าขาว" and "เมนูและส่วนอื่นของหน้ายังกดได้ ไม่พังทั้งแอป" — so this is a strip above the
 * router outlet and nothing is removed while it shows. It carries no retry of its own either: the
 * criterion's retry is "ปุ่มลองใหม่บนหน้าเดิม", the one each screen already has, and a second retry
 * button here would be a control nobody declared.
 */
@Component({
  selector: 'app-error-banner',
  template: `
    @if (message()) {
      <p class="error-banner" role="alert">{{ message() }}</p>
    }
  `,
  styles: `
    .error-banner {
      background: #fef2f2;
      border: 1px solid #fecaca;
      border-radius: 0.25rem;
      color: #b91c1c;
      margin: 0 0 1rem;
      padding: 0.75rem 1rem;
    }
  `,
})
export class ErrorBannerComponent {
  /** Null hides the banner entirely — the ordinary state, and the one AC-miniloan-120 returns to. */
  readonly message = input<string | null>(null);
}
