import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { InflationResponseDTO } from '../interfaces/Client.interfaces';
import { BaseCacheService } from './BaseCacheService';

@Injectable({ providedIn: 'root' })
export class ArgentinaAPIService extends BaseCacheService {
  private readonly baseUrl = `${environment.apiUrl}/inflation`;

  constructor(private http: HttpClient) {
    super();
  }

  getInflation(): Observable<InflationResponseDTO> {
    return this.getWithCache('inflation_cache', () =>
      this.http.get<InflationResponseDTO>(this.baseUrl),
    );
  }
}
