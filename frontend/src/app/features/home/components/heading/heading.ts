import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  inject,
  Output,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { catchError, map, of, switchMap } from 'rxjs';
import { AccountService } from '../../../accounts/services/AccountService';
import { TransactionService } from '../../../transactions/services/transaction-service';
import { DolarService } from '../../../../core/services/dolarService';
import { ArgentinaAPIService } from '../../../../core/services/ArgentinaAPIService';
import { InflationResponseDTO } from '../../../../core/interfaces/Client.interfaces';
import { SavingsGoalService } from '../../../savings-goals/service/savings-goal-service';

@Component({
  selector: 'heading',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './heading.html',
  styleUrl: './heading.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Heading {
  private readonly accountService = inject(AccountService);
  private readonly dolarService = inject(DolarService);
  private readonly argentinaAPIService = inject(ArgentinaAPIService);
  private readonly savingsGoalService = inject(SavingsGoalService);
  private readonly transactionService = inject(TransactionService);

  /** Bumps whenever goals or transactions change, so the savings figure stays fresh. */
  private readonly savingsRefreshKey = computed(
    () => this.savingsGoalService.refresh$() + this.transactionService.refresh$(),
  );

  /**
   * Both feeds start as `null` rather than as a zero-filled object, so "not
   * answered yet" stays distinguishable from "answered with 0". A failing
   * external API resolves to `null` too, which dismisses the placeholders
   * instead of leaving them shimmering forever.
   */
  private readonly dolarData = toSignal(
    this.dolarService.getDollarValue().pipe(catchError(() => of(null))),
    { initialValue: undefined },
  );

  private readonly inflationData = toSignal(
    this.argentinaAPIService
      .getInflation()
      .pipe(catchError(() => of(null as InflationResponseDTO | null))),
    { initialValue: undefined },
  );

  readonly dolarCompra = computed(() => this.dolarData()?.compra ?? 0);
  readonly dolarVenta = computed(() => this.dolarData()?.venta ?? 0);
  readonly inflationValue = computed(() => this.inflationData()?.value ?? 0);
  readonly inflationDate = computed(() => this.inflationData()?.date ?? '');

  readonly totalBalance = this.accountService.totalBalance;

  /**
   * Money set aside in savings goals. It already left the account balances, so it
   * is shown next to them rather than added in. Re-fetched whenever a goal or a
   * transaction changes, since both can move money in or out of a goal.
   */
  readonly totalSavings = toSignal(
    toObservable(this.savingsRefreshKey).pipe(
      switchMap(() =>
        this.savingsGoalService.getSummary().pipe(
          map((summary) => summary.totalSaved),
          catchError(() => of(0)),
        ),
      ),
    ),
    { initialValue: 0 },
  );

  /** Placeholders stay up only while a feed has not settled yet. */
  isLoading = computed(
    () => this.dolarData() === undefined || this.inflationData() === undefined,
  );

  @Output() readonly newAccount = new EventEmitter<void>();
  @Output() readonly newTransaction = new EventEmitter<void>();

  openNewAccountModal(): void {
    this.newAccount.emit();
  }

  openNewTransactionModal(): void {
    this.newTransaction.emit();
  }

  get userHasAccounts(): boolean {
    return this.accountService.userHasAccounts();
  }
}
