import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-transaction-list',
  imports: [],
  templateUrl: './transaction-list.html',
  styleUrl: './transaction-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TransactionList { }
