import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { BaseService } from '../../../core/services/BaseService';
import { RefreshableCrudService } from '../../../core/services/RefreshableCrudService.mixin';
import { BudgetDTO } from '../interfaces/BudgetDTO.interface';
import { BudgetProgressDTO } from '../interfaces/BudgetProgressDTO.interface';

@Injectable({
  providedIn: 'root',
})
export class BudgetService extends BaseService<BudgetDTO> {
  protected readonly baseUrl = `${environment.apiUrl}/budget`;

  private refreshable = new RefreshableCrudService();
  readonly refresh$ = this.refreshable.getRefreshSignal();

  constructor(http: HttpClient) {
    super(http);
  }

  override create(budget: BudgetDTO): Observable<BudgetDTO> {
    return this.refreshable.wrapWithRefresh(super.create(budget));
  }

  override update(budget: BudgetDTO): Observable<BudgetDTO> {
    return this.refreshable.wrapWithRefresh(super.update(budget));
  }

  override delete(id: string): Observable<void> {
    return this.refreshable.wrapWithRefresh(super.delete(id));
  }

  getProgress(year: number, month: number): Observable<BudgetProgressDTO[]> {
    return this.http.get<BudgetProgressDTO[]>(`${this.baseUrl}/progress`, {
      params: { year, month },
    });
  }
}
