import { tap } from 'rxjs';
import { Observable } from 'rxjs/internal/Observable';

export abstract class BaseCacheService {
  protected getWithCache<T>(
    key: string,
    fetchFn: () => Observable<T>,
    ttlMs: number = 3600000,
  ): Observable<T> {
    const cached = localStorage.getItem(key);
    if (cached) {
      const { data, expiry } = JSON.parse(cached);
      if (expiry > Date.now())
        return new Observable((sub) => {
          sub.next(data);
          sub.complete();
        });
    }

    return fetchFn().pipe(
      tap((data) =>
        localStorage.setItem(
          key,
          JSON.stringify({ data, expiry: Date.now() + ttlMs }),
        ),
      ),
    );
  }
}
