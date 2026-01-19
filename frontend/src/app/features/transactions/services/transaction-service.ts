import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { TransactionDTO } from '../interfaces/TransactionDTO.interface';
import { HttpClient } from '@angular/common/http';
import { MonthlyTransactionReportDTO } from '../interfaces/MonthlyTransactionReportDTO.interface';
import { LastMovesDTO } from '../interfaces/LastMovesDTO.interface';
import { DashboardFilter } from '../../dashboard/interfaces/dashboardFilter.interface';
import { DashBoardDTO } from '../../dashboard/interfaces/DashBoardDTO.interface';
import { MonthlyIncomeExpenseDTO } from '../interfaces/MonthlyIncomeExpenseDTO.interface';
import { TransactionFilter } from '../interfaces/TransactionFilter.interface';
import { BaseService } from '../../../core/services/BaseService';
import { RefreshableCrudService } from '../../../core/services/RefreshableCrudService.mixin';

@Injectable({
  providedIn: 'root'
})
export class TransactionService extends BaseService<TransactionDTO> {

  protected readonly baseUrl = `${environment.apiUrl}/transaction`;

  private refreshable = new RefreshableCrudService();
  readonly refresh$ = this.refreshable.getRefreshSignal();

  constructor(http: HttpClient) {
    super(http);
  }

  override create(transaction: TransactionDTO): Observable<TransactionDTO> {
    return this.refreshable.wrapWithRefresh(super.create(transaction));
  }

  override update(transaction: TransactionDTO): Observable<TransactionDTO> {
    return this.refreshable.wrapWithRefresh(super.update(transaction));
  }

  override delete(id: string): Observable<void> {
    return this.refreshable.wrapWithRefresh(super.delete(id));
  }

  getTransactionsByUser(): Observable<TransactionDTO[]> {
    return this.http.get<TransactionDTO[]>(`${this.baseUrl}/by-user`);
  }

  getMovementsOfThisMonth(): Observable<LastMovesDTO[]> {
    return this.http.get<LastMovesDTO[]>(`${this.baseUrl}/movements-this-month`);
  }

  getCurrentMonthlyReport(): Observable<MonthlyTransactionReportDTO> {
    return this.http.get<MonthlyTransactionReportDTO>(`${this.baseUrl}/monthly-balance`);
  }

  dashboard(filter: DashboardFilter): Observable<DashBoardDTO> {
    return this.http.post<DashBoardDTO>(`${this.baseUrl}/dashboard`, filter);
  }

  /**
 * Retrieves income and expense totals per month.
 * Optionally filtered by account ID.
 */
  getIncomeExpenseByMonth(accountId?: string) {
    return this.http.get<MonthlyIncomeExpenseDTO[]>(
      `${this.baseUrl}/income-expense-by-month`,
      {
        params: accountId ? { accountId } : {}
      }
    );
  }
}
