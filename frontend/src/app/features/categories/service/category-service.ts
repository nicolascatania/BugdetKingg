import { Injectable } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OptionDTO } from '../../../shared/models/OptionDTO.interface';
import { CategoryDTO } from '../interfaces/CategoryDTO.interface';
import { BaseFilter, PageResponse } from '../../../core/interfaces/GenericFilter.interfaces';

@Injectable({
  providedIn: 'root'
})
export class CategoryService {

  private readonly baseUrl = `${environment.apiUrl}/category`;

  constructor(private http: HttpClient) { }


  getOptions(): Observable<OptionDTO[]> {
    return this.http.get<OptionDTO[]>(`${this.baseUrl}/options`);
  }

  // category-service.ts
  search(filter: BaseFilter): Observable<PageResponse<CategoryDTO>> {
    return this.http.post<PageResponse<CategoryDTO>>(`${this.baseUrl}/search`, filter);
  }

  save(category: CategoryDTO): Observable<CategoryDTO> {
    if (category.id) {
      return this.http.put<CategoryDTO>(`${this.baseUrl}/${category.id}`, category);
    } else {
      return this.http.post<CategoryDTO>(this.baseUrl, category);
    }
  }

  delete(categoryId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${categoryId}`);
  }
}
