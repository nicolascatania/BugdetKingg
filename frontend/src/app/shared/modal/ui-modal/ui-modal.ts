import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
} from '@angular/core';

@Component({
  selector: 'ui-modal',
  standalone: true,
  templateUrl: './ui-modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UiModalComponent {
  @Input() title = '';
  @Input() open = false;
  @Input() maxWidth: string = 'max-w-lg';
  @Output() closed = new EventEmitter<void>();

  close(): void {
    this.closed.emit();
  }
}
