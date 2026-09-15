import { TranslocoDirective } from '@jsverse/transloco';
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
import { catchError, map, of, startWith, switchMap } from 'rxjs';
import { AccountService } from '../../../accounts/services/AccountService';
import { TransactionService } from '../../../transactions/services/transaction-service';
import { DolarService } from '../../../../core/services/dolarService';
import { ArgentinaAPIService } from '../../../../core/services/ArgentinaAPIService';
import { InflationResponseDTO } from '../../../../core/interfaces/Client.interfaces';
import { SavingsGoalService } from '../../../savings-goals/service/savings-goal-service';
import { AuthService } from '../../../../core/services/auth';
import { RegionService } from '../../../../core/regions/region.service';

@Component({
  selector: 'heading',
  standalone: true,
  imports: [CommonModule, TranslocoDirective],
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
  private readonly region = inject(RegionService);
  private readonly auth = inject(AuthService);

  /** Bumps whenever goals or transactions change, so the savings figure stays fresh. */
  private readonly savingsRefreshKey = computed(
    () => this.savingsGoalService.refresh$() + this.transactionService.refresh$(),
  );

  /**
   * Which market blocks this user's country unlocks (see `core/regions`). The
   * Argentine feeds below are only requested while their widget is enabled, so
   * a user elsewhere never pays for two external API calls they cannot see.
   */
  readonly showDollar = computed(() => this.region.hasWidget('usd-ars'));
  readonly showInflation = computed(() => this.region.hasWidget('inflation-indec'));
  readonly marketWidgets = this.region.widgets;
  readonly showMarket = computed(() => this.marketWidgets().length > 0);

  /**
   * Both feeds start as `undefined` ("not answered yet") and settle to data or
   * `null` (API failed), so placeholders can tell the two apart. A disabled
   * widget settles to `null` straight away without a request.
   */
  private readonly dolarData = toSignal(
    toObservable(this.showDollar).pipe(
      switchMap((enabled) =>
        enabled
          ? this.dolarService.getDollarValue().pipe(
              catchError(() => of(null)),
              startWith(undefined),
            )
          : of(null),
      ),
    ),
    { initialValue: undefined },
  );

  private readonly inflationData = toSignal(
    toObservable(this.showInflation).pipe(
      switchMap((enabled) =>
        enabled
          ? this.argentinaAPIService.getInflation().pipe(
              catchError(() => of(null as InflationResponseDTO | null)),
              startWith(undefined),
            )
          : of(null as InflationResponseDTO | null),
      ),
    ),
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

  /** Placeholders stay up only while an enabled feed has not settled yet. */
  isLoading = computed(
    () =>
      this.showMarket() &&
      (this.dolarData() === undefined || this.inflationData() === undefined),
  );

  @Output() readonly newAccount = new EventEmitter<void>();
  @Output() readonly newTransaction = new EventEmitter<void>();

  constructor() {
    // The market column keys off the profile's country; make sure it is on its way.
    this.auth.ensureCurrentUserLoaded();
  }

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
