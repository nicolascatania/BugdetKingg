import { signal, Signal } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

/**
 * Mixin providing change-notification logic for CRUD services.
 * Useful for services that need to notify other components/services when their data changes.
 *
 * Usage:
 * ```typescript
 * export class MyService extends BaseService<MyDTO> {
 *   private refreshable = new RefreshableCrudService();
 *   readonly refresh$ = this.refreshable.refresh$;
 * 
 *   override create(entity: MyDTO): Observable<MyDTO> {
 *     return this.refreshable.wrapWithRefresh(super.create(entity));
 *   }
 * }
 * ```
 */
export class RefreshableCrudService {
  private refreshSignal = signal(0);
  readonly refresh$: Signal<number> = this.refreshSignal.asReadonly();

  /**
   * Notifies every observer that data has changed.
   */
  protected notifyRefresh(): void {
    this.refreshSignal.update(v => v + 1);
  }

  /**
   * Wraps a CRUD observable so a refresh is emitted automatically.
   * @param observable Observable of the CRUD operation
   * @returns Observable that notifies a refresh once it completes
   */
  wrapWithRefresh<T>(observable: Observable<T>): Observable<T> {
    return observable.pipe(
      tap(() => this.notifyRefresh())
    );
  }

  /**
   * Returns the read-only change signal.
   */
  getRefreshSignal(): Signal<number> {
    return this.refresh$;
  }
}
