import { signal, Signal } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

/**
 * Mixin que proporciona lógica de notificación de cambios para servicios CRUD
 * Útil para servicios que necesitan notificar a otros componentes/servicios cuando sus datos cambian
 * 
 * Uso:
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
   * Notifica a todos los observadores que hay cambios
   */
  protected notifyRefresh(): void {
    this.refreshSignal.update(v => v + 1);
  }

  /**
   * Envuelve un observable CRUD con notificación automática
   * @param observable Observable de operación CRUD
   * @returns Observable con notificación de refresh al completarse
   */
  wrapWithRefresh<T>(observable: Observable<T>): Observable<T> {
    return observable.pipe(
      tap(() => this.notifyRefresh())
    );
  }

  /**
   * Obtiene el observable de cambios (read-only)
   */
  getRefreshSignal(): Signal<number> {
    return this.refresh$;
  }
}
