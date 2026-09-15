import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  LOCALE_ID,
  Output,
  inject,
} from '@angular/core';
import { TranslocoDirective } from '@jsverse/transloco';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { getMonthDateRange } from '../../utils/datesUtils';
import { monthAbbreviations } from '../../models/months.const';

/** Range emitted when a month is picked, ready to patch into a date-range FormGroup. */
export interface MonthQuickRange {
  from: string;
  to: string;
}

/**
 * Small button + popover menu that lets the user pick a month abbreviation
 * (current year) and get back the first/last day of that month, so callers
 * can patch their date-range filter form in one click instead of typing dates.
 */
@Component({
  selector: 'app-month-quick-picker',
  standalone: true,
  imports: [CommonModule, MatMenuModule, MatTooltipModule, TranslocoDirective],
  templateUrl: './month-quick-picker.html',
  styleUrl: './month-quick-picker.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MonthQuickPicker {
  /**
   * Whether the target date controls carry a time component
   * (native <input type="datetime-local">) instead of a plain date.
   * When true, ranges start at 00:00 and end at 23:59.
   */
  @Input() includeTime = false;

  @Output() rangeSelected = new EventEmitter<MonthQuickRange>();

  /** Abbreviated month names in the active locale. */
  months = monthAbbreviations(inject(LOCALE_ID));

  // The picker always targets the current year, hence the info tooltip.
  currentYear = new Date().getFullYear();

  selectMonth(monthIndex: number): void {
    const range = getMonthDateRange(monthIndex, this.currentYear, this.includeTime);
    this.rangeSelected.emit(range);
  }
}
