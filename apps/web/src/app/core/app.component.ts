import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { NavComponent } from './layout/nav.component';
import { ApiErrorService } from './services/api-error.service';
import { ErrorBannerComponent } from '../shared/components/error-banner.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, NavComponent, ErrorBannerComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  // mockup/conventions.json shell.productName, confirmed by aplus191
  protected readonly productName = 'ระบบสินเชื่อส่วนบุคคล miniloan';

  /**
   * FE-miniloan-029 · UC-miniloan-026. The shell owns the connectivity state and passes it to the
   * banner, which injects nothing — that is what keeps the banner in `shared` while the service and
   * the interceptor stay in `core`.
   *
   * <p>The banner lives HERE rather than on each screen because AC-miniloan-119 requires the rest of
   * the page to keep working while it shows, and because putting it on ten screens would be ten
   * copies of one sentence.
   */
  protected readonly apiError = inject(ApiErrorService).message;
}
