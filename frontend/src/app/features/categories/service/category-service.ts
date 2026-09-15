import { Injectable } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CategoryDTO } from '../interfaces/CategoryDTO.interface';
import { CategorySpendingDTO } from '../interfaces/CategorySpendingDTO.interface';
import { BaseService } from '../../../core/services/BaseService';

@Injectable({
  providedIn: 'root'
})
export class CategoryService extends BaseService<CategoryDTO> {

  protected readonly baseUrl = `${environment.apiUrl}/category`;

  constructor(http: HttpClient) {
    super(http);
  }

  /** Expenses on the category for one month plus its all-time total. */
  getSpending(id: string, year: number, month: number): Observable<CategorySpendingDTO> {
    return this.http.get<CategorySpendingDTO>(`${this.baseUrl}/${id}/spending`, { params: { year, month } });
  }

  save(category: CategoryDTO): Observable<CategoryDTO> {
    if (category.id) {
      return this.update(category);
    } else {
      return this.create(category);
    }
  }
}
