import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  HostListener,
  Input,
  OnDestroy,
  Output,
} from '@angular/core';

/**
 * Generic dialog shell used by every edit/create form in the app.
 *
 * Responsibilities kept here so no caller has to repeat them:
 * - locks page scroll while open
 * - closes on Escape and on backdrop click
 * - animates in (backdrop fade + panel lift) and stays scrollable on short
 *   viewports, rendering as a bottom sheet on phones
 */
@Component({
  selector: 'ui-modal',
  standalone: true,
  templateUrl: './ui-modal.html',
  styleUrl: './ui-modal.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UiModalComponent implements OnDestroy {
  @Input() title = '';
  @Input() maxWidth: string = 'max-w-lg';
  @Output() closed = new EventEmitter<void>();

  @Input()
  set open(value: boolean) {
    this._open = value;
    this.lockPageScroll(value);
  }
  get open(): boolean {
    return this._open;
  }
  private _open = false;

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this._open) {
      this.close();
    }
  }

  ngOnDestroy(): void {
    this.lockPageScroll(false);
  }

  close(): void {
    this.closed.emit();
  }

  /** Prevents the page behind the dialog from scrolling while it is open. */
  private lockPageScroll(locked: boolean): void {
    if (typeof document === 'undefined') {
      return;
    }
    document.body.style.overflow = locked ? 'hidden' : '';
  }
}
