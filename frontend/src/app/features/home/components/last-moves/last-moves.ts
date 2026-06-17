import { ChangeDetectionStrategy, Component, inject, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TransactionService } from '../../../transactions/services/transaction-service';
import { LastMovesDTO } from '../../../transactions/interfaces/LastMovesDTO.interface';
import { TransactionType } from '../../../../shared/models/TransactionType.enum';

@Component({
  selector: 'last-moves',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './last-moves.html',
  styleUrl: './last-moves.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LastMoves {
  private transactionService = inject(TransactionService);
  TRANSACTION_TYPES = TransactionType;
  
  txs = signal<LastMovesDTO[]>([]);
  loading = signal(true);

  constructor() {
    effect(() => {
      this.transactionService.refresh$();
      this.transactionService.getMovementsOfThisMonth().subscribe({
        next: (txs) => {
          this.txs.set(txs);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
    });
  }
}