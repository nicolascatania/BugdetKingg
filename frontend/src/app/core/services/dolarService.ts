import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { BaseCacheService } from './BaseCacheService';

@Injectable({ providedIn: 'root' })
export class DolarService extends BaseCacheService {
  private readonly baseUrl = `${environment.apiUrl}/dolar`;
  constructor(private http: HttpClient) {
    super();
  }

  getDollarValue(): Observable<any> {
    return this.getWithCache('dolar_cache', () =>
      this.http.get<any>(`${this.baseUrl}/cotizacion-oficial`),
    );
  }
}
