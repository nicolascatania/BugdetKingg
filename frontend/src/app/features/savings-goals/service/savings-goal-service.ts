import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { BaseService } from '../../../core/services/BaseService';
import { RefreshableCrudService } from '../../../core/services/RefreshableCrudService.mixin';
import { TransactionService } from '../../transactions/services/transaction-service';
import { SavingsGoalDTO } from '../interfaces/SavingsGoalDTO.interface';
import { SavingsGoalSummaryDTO } from '../interfaces/SavingsGoalSummaryDTO.interface';
import {
  SavingsGoalCloseDTO,
  SavingsGoalContributionDTO,
} from '../interfaces/SavingsGoalContributionDTO.interface';

@Injectable({
  providedIn: 'root',
})
export class SavingsGoalService extends BaseService<SavingsGoalDTO> {
  protected readonly baseUrl = `${environment.apiUrl}/savings-goal`;

  private readonly transactionService = inject(TransactionService);

  private refreshable = new RefreshableCrudService();
  readonly refresh$ = this.refreshable.getRefreshSignal();

  constructor(http: HttpClient) {
    super(http);
  }

  override create(goal: SavingsGoalDTO): Observable<SavingsGoalDTO> {
    return this.refreshable.wrapWithRefresh(super.create(goal));
  }

  override update(goal: SavingsGoalDTO): Observable<SavingsGoalDTO> {
    return this.refreshable.wrapWithRefresh(super.update(goal));
  }

  override delete(id: string): Observable<void> {
    return this.refreshable.wrapWithRefresh(super.delete(id));
  }

  getSummary(): Observable<SavingsGoalSummaryDTO> {
    return this.http.get<SavingsGoalSummaryDTO>(`${this.baseUrl}/summary`);
  }

  /** Moves money from an account into the goal. */
  deposit(id: string, dto: SavingsGoalContributionDTO): Observable<SavingsGoalDTO> {
    return this.moneyMovement(this.http.post<SavingsGoalDTO>(`${this.baseUrl}/${id}/deposit`, dto));
  }

  /** Moves money from the goal back into an account. */
  withdraw(id: string, dto: SavingsGoalContributionDTO): Observable<SavingsGoalDTO> {
    return this.moneyMovement(this.http.post<SavingsGoalDTO>(`${this.baseUrl}/${id}/withdraw`, dto));
  }

  /** Returns everything the goal holds to an account and freezes it. */
  close(id: string, dto: SavingsGoalCloseDTO): Observable<SavingsGoalDTO> {
    return this.moneyMovement(this.http.post<SavingsGoalDTO>(`${this.baseUrl}/${id}/close`, dto));
  }

  /**
   * Contributions change account balances and add transactions, so besides our own
   * refresh the transaction feed is notified — that is what account widgets listen to.
   */
  private moneyMovement(request$: Observable<SavingsGoalDTO>): Observable<SavingsGoalDTO> {
    return this.refreshable
      .wrapWithRefresh(request$)
      .pipe(tap(() => this.transactionService.notifyBalancesChanged()));
  }
}
