import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from '../../services/auth';
import { ThemeToggleComponent } from '../../../shared/components/theme-toggle/theme-toggle';
import { SiteFooterComponent } from '../../../shared/components/site-footer/site-footer';
import { LanguageSwitcherComponent } from '../../../shared/components/language-switcher/language-switcher';
import { TranslocoDirective } from '@jsverse/transloco';

/**
 * Shell for the marketing/legal routes (landing, terms): a slim top bar with
 * the brand, theme switch and a sign-in call to action, the routed page, and
 * the full site footer.
 *
 * Unlike `AuthLayout` it does not centre its content — these pages scroll.
 */
@Component({
  selector: 'app-public-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    ThemeToggleComponent,
    SiteFooterComponent,
    LanguageSwitcherComponent,
    TranslocoDirective,
  ],
  templateUrl: './public-layout.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublicLayout {
  private readonly auth = inject(AuthService);

  /**
   * A returning user with a live session gets sent straight into the app
   * instead of being asked to sign in again. Read once per navigation: the
   * token only changes through a full page load (see AuthService.logout).
   */
  readonly ctaRoute = this.auth.isLoggedIn() ? '/home' : '/login';
  readonly ctaLabelKey = this.auth.isLoggedIn() ? 'public.openApp' : 'public.signIn';
}
