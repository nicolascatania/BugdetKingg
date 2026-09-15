import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UiModalComponent } from '../../modal/ui-modal/ui-modal';
import { TranslocoDirective, TranslocoPipe } from '@jsverse/transloco';

/** One explained step/topic inside a tutorial modal; texts are translation keys. */
export interface TutorialSection {
  icon: string;
  headingKey: string;
  bodyKey: string;
}

/**
 * Generic "how does this screen work" modal, triggered by the info icon next to a
 * page title. Every feature page supplies its own title/intro/sections; this
 * component only owns the shell and layout.
 */
@Component({
  selector: 'app-tutorial-modal',
  standalone: true,
  imports: [UiModalComponent, CommonModule, TranslocoPipe, TranslocoDirective],
  templateUrl: './tutorial-modal.html',
  styleUrl: './tutorial-modal.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TutorialModal {
  title = input.required<string>();
  intro = input<string>('');
  sections = input.required<TutorialSection[]>();

  closed = output<void>();

  close(): void {
    this.closed.emit();
  }
}
