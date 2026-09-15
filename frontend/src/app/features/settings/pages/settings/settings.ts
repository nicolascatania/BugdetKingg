import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';
import { catchError, of } from 'rxjs';
import { ThemeService, Theme } from '../../../../core/services/theme.service';
import { LanguageService } from '../../../../core/i18n/language.service';
import { LanguageCode } from '../../../../core/i18n/languages';
import { RegionService } from '../../../../core/regions/region.service';
import { CountryOption, CountryService } from '../../../../core/services/country.service';
import { AuthService } from '../../../../core/services/auth';
import { NotificationService } from '../../../../core/services/NotificationService';
import { RevealDirective } from '../../../../shared/directives/reveal.directive';

/** One selectable appearance option: the theme value plus how it is presented. */
interface ThemeOption {
  readonly value: Theme;
  readonly labelKey: string;
  readonly hintKey: string;
  readonly icon: string;
}

/**
 * User preferences. Appearance and language are device settings and persist
 * in localStorage through their services; country is part of the profile and
 * persists on the backend (`PATCH /me`) so it follows the user across devices.
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
  protected readonly region = inject(RegionService);
  private readonly countryService = inject(CountryService);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly transloco = inject(TranslocoService);

  readonly themeOptions: readonly ThemeOption[] = [
    { value: 'light', labelKey: 'settings.appearance.light', hintKey: 'settings.appearance.lightHint', icon: 'fa-sun' },
    { value: 'dark', labelKey: 'settings.appearance.dark', hintKey: 'settings.appearance.darkHint', icon: 'fa-moon' },
  ];

  /** Country picker entries; `undefined` while loading, `null` if the list could not be fetched. */
  readonly countries = toSignal(
    this.countryService.getOptions().pipe(catchError(() => of(null as CountryOption[] | null))),
    { initialValue: undefined },
  );

  /** True while a country change is in flight; the picker is disabled meanwhile. */
  readonly savingCountry = signal(false);

  constructor() {
    // The picker's current value comes from the profile.
    this.auth.ensureCurrentUserLoaded();
  }

  selectTheme(value: Theme): void {
    this.theme.setTheme(value);
  }

  /** Persists the choice and reloads the page — see LanguageService. */
  selectLanguage(code: LanguageCode): void {
    this.language.setLanguage(code);
  }

  /** Native select handler; the empty option maps to "no country". */
  selectCountry(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const country = select.value === '' ? null : select.value;
    if (country === this.region.country()) {
      return;
    }

    this.savingCountry.set(true);
    this.region.setCountry(country).subscribe({
      next: () => {
        this.savingCountry.set(false);
        this.notifications.success(this.transloco.translate('settings.region.saved'));
      },
      error: () => {
        this.savingCountry.set(false);
        // The profile signal did not change, so the binding will not repaint: roll the DOM back by hand.
        select.value = this.region.country() ?? '';
        this.notifications.error(this.transloco.translate('settings.region.error'));
      },
    });
  }
}
