import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { BaseService } from '../../../core/services/BaseService';
import { RefreshableCrudService } from '../../../core/services/RefreshableCrudService.mixin';
import { RecurringTransactionDTO } from '../interfaces/RecurringTransactionDTO.interface';

@Injectable({
  providedIn: 'root',
})
export class RecurringTransactionService extends BaseService<RecurringTransactionDTO> {
  protected readonly baseUrl = `${environment.apiUrl}/recurring-transaction`;

  private refreshable = new RefreshableCrudService();
  readonly refresh$ = this.refreshable.getRefreshSignal();

  constructor(http: HttpClient) {
    super(http);
  }

  override create(template: RecurringTransactionDTO): Observable<RecurringTransactionDTO> {
    return this.refreshable.wrapWithRefresh(super.create(template));
  }

  override update(template: RecurringTransactionDTO): Observable<RecurringTransactionDTO> {
    return this.refreshable.wrapWithRefresh(super.update(template));
  }

  override delete(id: string): Observable<void> {
    return this.refreshable.wrapWithRefresh(super.delete(id));
  }

  runNow(id: string): Observable<RecurringTransactionDTO> {
    return this.refreshable.wrapWithRefresh(
      this.http.post<RecurringTransactionDTO>(`${this.baseUrl}/${id}/run-now`, {}),
    );
  }

  getUpcoming(): Observable<RecurringTransactionDTO[]> {
    return this.http.get<RecurringTransactionDTO[]>(`${this.baseUrl}/upcoming`);
  }
}
