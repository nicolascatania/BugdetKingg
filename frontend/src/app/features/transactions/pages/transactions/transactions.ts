import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'transactions',
  imports: [],
  templateUrl: './transactions.html',
  styleUrl: './transactions.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Transactions { }
