import { computed, effect, Injectable, signal } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AccountDTO } from '../interfaces/AccountDTO.interfaces';
import { TransactionService } from '../../transactions/services/transaction-service';
import { BaseService } from '../../../core/services/BaseService';
import { RefreshableCrudService } from '../../../core/services/RefreshableCrudService.mixin';

@Injectable({
  providedIn: 'root'
})
export class AccountService extends BaseService<AccountDTO> {

  protected readonly baseUrl = `${environment.apiUrl}/account`;

  private _accounts = signal<AccountDTO[]>([]);
  readonly accounts = this._accounts.asReadonly();

  private refreshable = new RefreshableCrudService();
  readonly refresh$ = this.refreshable.getRefreshSignal();

  constructor(
    http: HttpClient,
    private transactionService: TransactionService
  ) {
    super(http);
    effect(() => {
      transactionService.refresh$();
      this.loadAccounts();
    });
  }

  totalBalance = computed(() =>
    this._accounts().reduce(
      (sum, acc) => sum + acc.balance,
      0
    )
  );

  loadAccounts(): void {
    this.http
      .get<AccountDTO[]>(`${this.baseUrl}/by-user`)
      .subscribe(accs => this._accounts.set(accs));
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
}