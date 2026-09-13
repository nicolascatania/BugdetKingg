import { computed, effect, Injectable, signal } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AccountDTO } from '../interfaces/AccountDTO.interfaces';
import { TransactionService } from '../../transactions/services/transaction-service';
import { BaseService } from '../../../core/services/BaseService';
import { RefreshableCrudService } from '../../../core/services/RefreshableCrudService.mixin';

@Injectable({
  providedIn: 'root',
})
export class AccountService extends BaseService<AccountDTO> {
  protected readonly baseUrl = `${environment.apiUrl}/account`;

  private _accounts = signal<AccountDTO[]>([]);
  readonly accounts = this._accounts.asReadonly();

  /**
   * True while an account request is in flight. Starts as `true` so the very
   * first paint shows placeholders instead of a flash of the empty state:
   * consumers must not infer loading from an empty list, because "no accounts
   * yet" and "accounts not fetched yet" are different states.
   */
  private _loading = signal(true);
  readonly loading = this._loading.asReadonly();

  private refreshable = new RefreshableCrudService();
  readonly refresh$ = this.refreshable.getRefreshSignal();

  constructor(
    http: HttpClient,
    private transactionService: TransactionService,
  ) {
    super(http);
    effect(
      () => {
        this.refresh$();
        this.transactionService.refresh$();
        this.loadAccounts();
      },
      { allowSignalWrites: true },
    );
  }

  totalBalance = computed(() =>
    this._accounts().reduce((sum, acc) => sum + acc.balance, 0),
  );

  private loadAccounts(): void {
    this._loading.set(true);
    this.http.get<AccountDTO[]>(`${this.baseUrl}/by-user`).subscribe({
      next: (accs) => {
        this._accounts.set(accs);
        this._loading.set(false);
      },
      error: () => this._loading.set(false),
    });
  }

  override create(account: AccountDTO) {
    return this.refreshable.wrapWithRefresh(super.create(account));
  }

  override update(account: AccountDTO) {
    return this.refreshable.wrapWithRefresh(super.update(account));
  }

  override delete(id: string) {
    return this.refreshable.wrapWithRefresh(super.delete(id));
  }

  userHasAccounts(): boolean {
    return this._accounts().length > 0;
  }

  /**
   * Forces a reload of the account list. Needed after a bulk CSV import, which is
   * committed through a separate endpoint and never goes through create().
   */
  refreshAccounts(): void {
    this.loadAccounts();
  }
}
