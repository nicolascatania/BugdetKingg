import { computed, effect, Injectable, signal } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AccountDTO } from '../interfaces/AccountDTO.interfaces';
import { TransactionService } from '../../transactions/services/transaction-service';
import { OptionDTO } from '../../../shared/models/OptionDTO.interface';

@Injectable({
  providedIn: 'root'
})
export class AccountService {

  private readonly baseUrl = `${environment.apiUrl}/account`;

  private _accounts = signal<AccountDTO[]>([]);
  readonly accounts = this._accounts.asReadonly();

  constructor(
    private http: HttpClient,
    private transactionService: TransactionService
  ) {
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

  create(account: AccountDTO) {
    return this.http.post<AccountDTO>(this.baseUrl, account).pipe(
      tap(() => {
        this.loadAccounts(),
          this.transactionService.notifyRefresh();
      }
      )
    );
  }

  update(account: AccountDTO) {
    return this.http.put<AccountDTO>(`${this.baseUrl}/${account.id}`, account).pipe(
      tap(() => this.loadAccounts())
    );
  }

  delete(id: string) {
    return this.http.delete(`${this.baseUrl}/${id}`).pipe(
      tap(() => this.loadAccounts())
    );
  }


  getOptions(): Observable<OptionDTO[]> {
    return this.http.get<OptionDTO[]>(`${this.baseUrl}/options`);
  }
}