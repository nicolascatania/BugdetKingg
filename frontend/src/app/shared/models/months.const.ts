import { FormStyle, TranslationWidth, getLocaleMonthNames } from '@angular/common';

export interface MonthOption {
  /** 1-based month number, as the backend expects it. */
  value: number;
  label: string;
}

/**
 * The twelve months named in the given locale (`LOCALE_ID`), so "January"
 * becomes "Enero", "Januar" or "Styczeń" without a translation key per month.
 * Standalone form: the name as it appears on its own, not inside a date.
 */
export function monthOptions(locale: string): MonthOption[] {
  const names = getLocaleMonthNames(locale, FormStyle.Standalone, TranslationWidth.Wide);
  return names.map((label, index) => ({ value: index + 1, label: capitalize(label) }));
}

/** Abbreviated month names ("Jan", "Ene", ...) in the given locale. */
export function monthAbbreviations(locale: string): string[] {
  return getLocaleMonthNames(locale, FormStyle.Standalone, TranslationWidth.Abbreviated).map(capitalize);
}

/** Some locales (Spanish, Polish) list month names in lowercase; menus read better capitalised. */
function capitalize(value: string): string {
  return value.charAt(0).toUpperCase() + value.slice(1);
}
