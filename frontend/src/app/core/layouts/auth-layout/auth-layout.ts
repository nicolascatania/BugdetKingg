import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ThemeToggleComponent } from '../../../shared/components/theme-toggle/theme-toggle';
import { SiteFooterComponent } from '../../../shared/components/site-footer/site-footer';
import { LanguageSwitcherComponent } from '../../../shared/components/language-switcher/language-switcher';
import { TranslocoDirective } from '@jsverse/transloco';

/** Shell for the public (login/register) routes: animated backdrop + centred content + compact footer. */
@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [RouterOutlet, ThemeToggleComponent, SiteFooterComponent, LanguageSwitcherComponent, TranslocoDirective],
  templateUrl: './auth-layout.html',
  styleUrl: './auth-layout.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuthLayout {}
