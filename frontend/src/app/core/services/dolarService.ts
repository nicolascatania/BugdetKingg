import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class DolarService {

  protected readonly baseUrl = `${environment.apiUrl}/dolar`;

  constructor(private http: HttpClient) { }


  getDolarCompraVenta(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/cotizacion-oficial`);
  }
}
