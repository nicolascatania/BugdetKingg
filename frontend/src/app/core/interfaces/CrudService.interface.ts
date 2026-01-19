import { Observable } from 'rxjs';
import { PageResponse, BaseFilter } from './GenericFilter.interfaces';

/**
 * Generic CRUD Service Interface for RESTful operations
 * Provides a standard contract for data access operations
 */
export interface ICrudService<T> {
  /**
   * Create a new entity
   */
  create(entity: T): Observable<T>;

  /**
   * Update an existing entity
   */
  update(entity: T): Observable<T>;

  /**
   * Delete an entity by ID
   */
  delete(id: string): Observable<void>;

  /**
   * Search entities with pagination and filtering
   */
  search(filter: BaseFilter): Observable<PageResponse<T>>;
}
