import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { TransactionService } from '../../../transactions/services/transaction-service';
import { TransactionDTO } from '../../../transactions/interfaces/TransactionDTO.interface';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'last-moves',
  imports: [CommonModule],
  templateUrl: './last-moves.html',
  styleUrl: './last-moves.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LastMoves {

  txs = signal<TransactionDTO[]>([]);



  constructor(private transactionService: TransactionService) { }


  ngOnInit(): void {
    this.transactionService.getMovementsOfThisMonth().subscribe({
      next: (txs) => {
        this.txs.set(txs);
      },
      error: (err) => {
        console.error('Error fetching last movements', err);
      }
    });
  }

}
