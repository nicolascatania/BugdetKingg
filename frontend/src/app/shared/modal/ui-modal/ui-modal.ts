import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'ui-modal',
  standalone: true,
  templateUrl: './ui-modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UiModalComponent {
  @Input() title = '';
  @Input() open = false;

  @Output() closed = new EventEmitter<void>();

  close(): void {
    this.closed.emit();
  }
}
