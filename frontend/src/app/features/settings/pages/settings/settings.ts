import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslocoDirective } from '@jsverse/transloco';
import { ThemeService, Theme } from '../../../../core/services/theme.service';
import { LanguageService } from '../../../../core/i18n/language.service';
import { LanguageCode } from '../../../../core/i18n/languages';
import { RevealDirective } from '../../../../shared/directives/reveal.directive';

/** One selectable appearance option: the theme value plus how it is presented. */
interface ThemeOption {
  readonly value: Theme;
  readonly labelKey: string;
  readonly hintKey: string;
  readonly icon: string;
}

/**
 * User preferences. Today: appearance (light/dark) and language. Both settings
 * already live in their services and persist in localStorage; this page only
 * gives them a proper home instead of the sidebar corner, and a place for the
 * options that come later (currency, notifications).
 */
@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, TranslocoDirective, RevealDirective],
  templateUrl: './settings.html',
  styleUrl: './settings.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Settings {
  protected readonly theme = inject(ThemeService);
  protected readonly language = inject(LanguageService);

  readonly themeOptions: readonly ThemeOption[] = [
    { value: 'light', labelKey: 'settings.appearance.light', hintKey: 'settings.appearance.lightHint', icon: 'fa-sun' },
    { value: 'dark', labelKey: 'settings.appearance.dark', hintKey: 'settings.appearance.darkHint', icon: 'fa-moon' },
  ];

  selectTheme(value: Theme): void {
    this.theme.setTheme(value);
  }

  /** Persists the choice and reloads the page — see LanguageService. */
  selectLanguage(code: LanguageCode): void {
    this.language.setLanguage(code);
  }
}
