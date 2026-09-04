import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { NavComponent } from './layout/nav.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, NavComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  // mockup/conventions.json shell.productName, confirmed by aplus191
  protected readonly productName = 'ระบบสินเชื่อส่วนบุคคล miniloan';
}
