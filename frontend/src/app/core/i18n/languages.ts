/**
 * Languages the UI ships in. Adding one means: a `public/i18n/<code>.json`
 * file, an entry here, and its Angular locale data registered in
 * `app.config.ts` (see `registerLocaleData`).
 */
export type LanguageCode = 'en' | 'es' | 'de' | 'pl';

export interface Language {
  readonly code: LanguageCode;
  /** Name in its own language, as language menus conventionally show it. */
  readonly nativeName: string;
  /**
   * Angular locale id driving `date`, `number` and `currency` pipes. Spanish
   * maps to Argentina since that is where the app is aimed (and where ARS
   * formats as "$ 1.234,56").
   */
  readonly locale: string;
}

export const LANGUAGES: readonly Language[] = [
  { code: 'en', nativeName: 'English', locale: 'en-US' },
  { code: 'es', nativeName: 'Español', locale: 'es-AR' },
  { code: 'de', nativeName: 'Deutsch', locale: 'de' },
  { code: 'pl', nativeName: 'Polski', locale: 'pl' },
];

export const DEFAULT_LANGUAGE: LanguageCode = 'en';

/** `localStorage` key holding the user's explicit choice; absent while the browser decides. */
export const LANGUAGE_STORAGE_KEY = 'budgetking-lang';

export function isLanguageCode(value: unknown): value is LanguageCode {
  return LANGUAGES.some((language) => language.code === value);
}

/**
 * Picks the UI language before Angular boots, so the very first render (and
 * `LOCALE_ID`, which is fixed at bootstrap) already match it.
 *
 * Order: the stored choice, then the browser's preferred languages
 * (`navigator.languages`, most preferred first, matched on the primary
 * subtag so `de-AT` still lands on German), then English.
 */
export function resolveInitialLanguage(): LanguageCode {
  try {
    const stored = localStorage.getItem(LANGUAGE_STORAGE_KEY);
    if (isLanguageCode(stored)) {
      return stored;
    }
  } catch {
    // Private browsing can block localStorage; fall through to detection.
  }

  const preferred = typeof navigator !== 'undefined' ? navigator.languages ?? [navigator.language] : [];
  for (const tag of preferred) {
    const primary = tag?.toLowerCase().split('-')[0];
    if (isLanguageCode(primary)) {
      return primary;
    }
  }

  return DEFAULT_LANGUAGE;
}

export function localeFor(code: LanguageCode): string {
  return LANGUAGES.find((language) => language.code === code)?.locale ?? 'en-US';
}
