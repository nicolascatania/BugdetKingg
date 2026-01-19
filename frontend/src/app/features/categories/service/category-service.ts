import { Injectable } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CategoryDTO } from '../interfaces/CategoryDTO.interface';
import { BaseService } from '../../../core/services/BaseService';

@Injectable({
  providedIn: 'root'
})
export class CategoryService extends BaseService<CategoryDTO> {

  protected readonly baseUrl = `${environment.apiUrl}/category`;

  constructor(http: HttpClient) {
    super(http);
  }

  save(category: CategoryDTO): Observable<CategoryDTO> {
    if (category.id) {
      return this.update(category);
    } else {
      return this.create(category);
    }
  }
}
