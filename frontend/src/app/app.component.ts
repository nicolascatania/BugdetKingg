import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ThemeService } from './core/services/theme.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent {
  title = 'BudgetKing';

  // Injected at the root so the theme is resolved and applied for the whole
  // session, independent of which layout happens to render the toggle.
  private readonly themeService = inject(ThemeService);
}
