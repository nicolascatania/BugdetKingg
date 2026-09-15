import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth';
import { RevealDirective } from '../../../../shared/directives/reveal.directive';
import { TranslocoDirective } from '@jsverse/transloco';

/** One tile in the features grid; `key` selects `landing.features.<key>.{title,description}`. */
interface Feature {
  readonly icon: string;
  readonly key: string;
}

/**
 * Public landing page: what the app does, how to start, and how personal data
 * is treated. Copy lives in the translation files; the lists here only carry
 * icons and keys so the template stays a single loop per section.
 */
@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink, RevealDirective, TranslocoDirective],
  templateUrl: './landing.html',
  styleUrl: './landing.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Landing {
  private readonly auth = inject(AuthService);

  /** Signed-in visitors go straight to the app; everyone else to sign-in. */
  readonly ctaRoute = this.auth.isLoggedIn() ? '/home' : '/login';
  readonly ctaLabelKey = this.auth.isLoggedIn() ? 'landing.hero.openApp' : 'landing.hero.signInGoogle';

  readonly repositoryUrl = 'https://github.com/nicolascatania/BugdetKingg';

  /** Mirrors the sidebar's navigation, one tile per feature area. */
  readonly features: readonly Feature[] = [
    { icon: 'fa-wallet', key: 'accounts' },
    { icon: 'fa-arrow-right-arrow-left', key: 'transactions' },
    { icon: 'fa-rotate', key: 'recurring' },
    { icon: 'fa-chart-pie', key: 'budgets' },
    { icon: 'fa-piggy-bank', key: 'savings' },
    { icon: 'fa-chart-line', key: 'dashboard' },
    { icon: 'fa-file-csv', key: 'csv' },
    { icon: 'fa-moon', key: 'themes' },
  ];

  /** Keys of `landing.steps.*`, in order. */
  readonly steps: readonly string[] = ['signIn', 'setUp', 'track'];
}
