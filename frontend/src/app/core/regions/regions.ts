/**
 * Region registry: which country-specific data the UI shows for a given
 * country. Country is a user profile setting (`UserProfile.country`, ISO
 * 3166-1 alpha-2); it is *not* the UI language and *not* a currency.
 *
 * Adding a country means one entry here plus the widget(s) it lists. Any
 * country without an entry shows nothing region-specific, which is the
 * intended default for a user who has not picked one yet.
 */

/** A region-specific block the home heading can render. */
export type MarketWidget = 'usd-ars' | 'inflation-indec';

export interface Region {
  /** ISO 3166-1 alpha-2 code. */
  readonly country: string;
  /** Market widgets rendered in the home heading, in order. */
  readonly widgets: readonly MarketWidget[];
}

export const REGIONS: readonly Region[] = [
  { country: 'AR', widgets: ['usd-ars', 'inflation-indec'] },
];

/** Widgets for a country; empty for an unknown or unset one. */
export function widgetsFor(country: string | null | undefined): readonly MarketWidget[] {
  return REGIONS.find((region) => region.country === country)?.widgets ?? [];
}
