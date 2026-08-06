import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { ThemeService } from '../../../core/services/theme.service';

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
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button
      type="button"
      (click)="theme.toggle()"
      [attr.aria-label]="theme.isDark() ? 'Switch to light theme' : 'Switch to dark theme'"
      [attr.aria-pressed]="theme.isDark()"
      [title]="theme.isDark() ? 'Light theme' : 'Dark theme'"
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
        <span class="sr-only">Toggle theme</span>
      }
    </button>
  `,
})
export class ThemeToggleComponent {
  /** Reserved for placements that also want a visible text label. */
  readonly showLabel = input<boolean>(false);

  protected readonly theme = inject(ThemeService);
}
