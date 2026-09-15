import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageService } from '../../../core/i18n/language.service';
import { LanguageCode, isLanguageCode } from '../../../core/i18n/languages';

/**
 * Language picker: a native `<select>` so it works with keyboard, screen
 * readers and on phones without any custom menu code. Each option shows the
 * language's own name (English, Español, ...), as language menus should.
 *
 * Selecting a language persists it and reloads the page — see LanguageService.
 */
@Component({
  selector: 'app-language-switcher',
  standalone: true,
  imports: [TranslocoPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <label class="relative block">
      <span class="sr-only">{{ 'common.language' | transloco }}</span>
      <i
        class="fa-solid fa-globe pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-xs text-muted"
        aria-hidden="true"
      ></i>
      <select
        class="field-select h-11 w-auto min-w-[8.5rem] cursor-pointer py-0 pl-9 text-sm font-medium"
        [value]="language.current()"
        (change)="onChange($event)"
      >
        @for (option of language.languages; track option.code) {
          <option [value]="option.code" [lang]="option.code">{{ option.nativeName }}</option>
        }
      </select>
    </label>
  `,
})
export class LanguageSwitcherComponent {
  readonly language = inject(LanguageService);

  onChange(event: Event): void {
    const code = (event.target as HTMLSelectElement).value;
    if (isLanguageCode(code)) {
      this.language.setLanguage(code as LanguageCode);
    }
  }
}
