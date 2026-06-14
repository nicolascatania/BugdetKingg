import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { InflationResponseDTO } from '../interfaces/Client.interfaces';

@Injectable({ providedIn: 'root' })
export class ArgentinaAPIService {
  protected readonly baseUrl = `${environment.apiUrl}/inflation`;

  constructor(private http: HttpClient) {}

  getInflation(): Observable<InflationResponseDTO> {
    return this.http.get<InflationResponseDTO>(`${this.baseUrl}`);
  }
}
