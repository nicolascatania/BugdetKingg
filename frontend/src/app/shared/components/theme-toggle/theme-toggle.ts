import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { ThemeService } from '../../../core/services/theme.service';
import { TranslocoPipe } from '@jsverse/transloco';

/**
 * Light/dark switch.
 *
 * Renders both icons stacked and cross-fades/rotates between them, so the
 * change of state is animated rather than instantaneous. The label is exposed
 * to assistive technology through `aria-label`, since the control is icon-only.
 */
@Component({
  selector: 'app-theme-toggle',
  standalone: true,
  imports: [TranslocoPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button
      type="button"
      (click)="theme.toggle()"
      [attr.aria-label]="(theme.isDark() ? 'theme.switchToLight' : 'theme.switchToDark') | transloco"
      [attr.aria-pressed]="theme.isDark()"
      [title]="(theme.isDark() ? 'theme.light' : 'theme.dark') | transloco"
      class="group relative flex h-11 w-11 shrink-0 items-center justify-center overflow-hidden
             rounded-xl border border-line bg-surface-2 text-muted
             transition-all duration-300 ease-smooth
             hover:border-brand/40 hover:text-brand active:scale-95"
    >
      <!-- Glow that blooms out of the button on hover. -->
      <span
        class="absolute inset-0 scale-0 rounded-xl bg-brand/10 transition-transform duration-500 ease-smooth group-hover:scale-100"
      ></span>

      <i
        class="fa-solid fa-sun absolute text-sm transition-all duration-500 ease-spring"
        [class.opacity-0]="theme.isDark()"
        [class.opacity-100]="!theme.isDark()"
        [class.rotate-90]="theme.isDark()"
        [class.scale-50]="theme.isDark()"
      ></i>

      <i
        class="fa-solid fa-moon absolute text-sm transition-all duration-500 ease-spring"
        [class.opacity-0]="!theme.isDark()"
        [class.opacity-100]="theme.isDark()"
        [class.-rotate-90]="!theme.isDark()"
        [class.scale-50]="!theme.isDark()"
      ></i>

      @if (showLabel()) {
        <span class="sr-only">{{ 'theme.toggle' | transloco }}</span>
      }
    </button>
  `,
})
export class ThemeToggleComponent {
  /** Reserved for placements that also want a visible text label. */
  readonly showLabel = input<boolean>(false);

  protected readonly theme = inject(ThemeService);
}
