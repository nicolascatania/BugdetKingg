import { ChangeDetectionStrategy, Component, effect, signal } from '@angular/core';
import { TransactionService } from '../../../transactions/services/transaction-service';
import { TransactionDTO } from '../../../transactions/interfaces/TransactionDTO.interface';
import { CommonModule } from '@angular/common';
import { LastMovesDTO } from '../../../transactions/interfaces/LastMovesDTO.interface';

@Component({
  selector: 'last-moves',
  imports: [CommonModule],
  templateUrl: './last-moves.html',
  styleUrl: './last-moves.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LastMoves {

  txs = signal<LastMovesDTO[]>([]);

  constructor(private transactionService: TransactionService) {
    effect(() => {
      this.transactionService.refresh$();

      this.transactionService
        .getMovementsOfThisMonth()
        .subscribe(txs => this.txs.set(txs));
    });
  }
}

