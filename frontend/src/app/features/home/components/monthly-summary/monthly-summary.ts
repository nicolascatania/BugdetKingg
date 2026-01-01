import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'monthly-summary',
  imports: [],
  templateUrl: './monthly-summary.html',
  styleUrl: './monthly-summary.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MonthlySummary { }
