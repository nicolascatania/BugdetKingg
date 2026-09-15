import { TranslocoDirective } from '@jsverse/transloco';
import { ChangeDetectionStrategy, Component, inject, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TransactionService } from '../../../transactions/services/transaction-service';
import { LastMovesDTO } from '../../../transactions/interfaces/LastMovesDTO.interface';
import {
  transactionAmountClass,
  transactionAmountSign,
  transactionTypeChip,
  transactionTypeLabel,
} from '../../../../shared/utils/transactionType.util';

@Component({
  selector: 'last-moves',
  standalone: true,
  imports: [CommonModule, TranslocoDirective],
  templateUrl: './last-moves.html',
  styleUrl: './last-moves.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LastMoves {
  private transactionService = inject(TransactionService);
  /** Type presentation rules are shared so every list renders a type the same way. */
  readonly typeChip = transactionTypeChip;
  readonly typeLabel = transactionTypeLabel;
  readonly amountClass = transactionAmountClass;
  readonly amountSign = transactionAmountSign;

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