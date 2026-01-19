import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ICrudService } from '../interfaces/CrudService.interface';
import { PageResponse, BaseFilter } from '../interfaces/GenericFilter.interfaces';
import { OptionDTO } from '../../shared/models/OptionDTO.interface';

/**
 * Base service class implementing ICrudService interface.
 * Provides common CRUD operations for all domain services.
 * 
 * Usage:
 * ```typescript
 * @Injectable({ providedIn: 'root' })
 * export class MyService extends BaseService<MyDTO> {
 *   protected readonly baseUrl = `${environment.apiUrl}/my-endpoint`;
 *   
 *   constructor(http: HttpClient) {
 *     super(http);
 *   }
 * }
 * ```
 */
export abstract class BaseService<T> implements ICrudService<T> {
  protected abstract readonly baseUrl: string;

  constructor(protected http: HttpClient) {}

  /**
   * Create a new entity
   */
  create(entity: T): Observable<T> {
    return this.http.post<T>(this.baseUrl, entity);
  }

  /**
   * Update an existing entity
   */
  update(entity: any): Observable<T> {
    return this.http.put<T>(`${this.baseUrl}/${entity.id}`, entity);
  }

  /**
   * Delete an entity by ID
   */
  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  /**
   * Search entities with pagination and filtering
   */
  search(filter: BaseFilter): Observable<PageResponse<T>> {
    return this.http.post<PageResponse<T>>(`${this.baseUrl}/search`, filter);
  }

  /**
   * Get available options for dropdowns/selects
   * Returns a list of key-value pairs for the entity
   */
  getOptions(): Observable<OptionDTO[]> {
    return this.http.get<OptionDTO[]>(`${this.baseUrl}/options`);
  }
}
