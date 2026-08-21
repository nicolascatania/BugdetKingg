import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { BaseService } from '../../../core/services/BaseService';
import { RefreshableCrudService } from '../../../core/services/RefreshableCrudService.mixin';
import { SavingsGoalDTO } from '../interfaces/SavingsGoalDTO.interface';
import { SavingsGoalSummaryDTO } from '../interfaces/SavingsGoalSummaryDTO.interface';

@Injectable({
  providedIn: 'root',
})
export class SavingsGoalService extends BaseService<SavingsGoalDTO> {
  protected readonly baseUrl = `${environment.apiUrl}/savings-goal`;

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
}
