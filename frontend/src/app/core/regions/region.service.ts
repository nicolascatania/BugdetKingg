import { Injectable, computed, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth';
import { UserProfile } from '../interfaces/UserProfile.interface';
import { MarketWidget, widgetsFor } from './regions';

/**
 * The signed-in user's country and what it unlocks in the UI.
 *
 * The country itself lives on the profile (`AuthService.currentUser`) so it
 * follows the user across devices; this service only derives from it. While
 * the profile has not loaded, or the user never chose a country, every
 * derived value is "nothing region-specific" — the deliberate default.
 */
@Injectable({ providedIn: 'root' })
export class RegionService {
  private readonly auth = inject(AuthService);

  /** ISO 3166-1 alpha-2 code, or null while unset or not loaded yet. */
  readonly country = computed(() => this.auth.currentUser()?.country ?? null);

  /** Region-specific blocks the home heading should render. */
  readonly widgets = computed(() => widgetsFor(this.country()));

  /** Whether a given widget is enabled for the user's country. */
  hasWidget(widget: MarketWidget): boolean {
    return this.widgets().includes(widget);
  }

  /** Persists the choice on the profile; `null` clears it. */
  setCountry(country: string | null): Observable<UserProfile> {
    return this.auth.updateProfile({ country });
  }
}
