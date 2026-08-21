import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ImportPreviewDTO } from '../interfaces/ImportPreviewDTO.interface';
import { TransactionFilter } from '../interfaces/TransactionFilter.interface';

@Injectable({
  providedIn: 'root',
})
export class TransactionImportExportService {
  private readonly baseUrl = `${environment.apiUrl}/transaction`;

  constructor(private http: HttpClient) {}

  previewImport(file: File): Observable<ImportPreviewDTO> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ImportPreviewDTO>(`${this.baseUrl}/import/preview`, formData);
  }

  commitImport(file: File, accountId: string): Observable<ImportPreviewDTO> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('accountId', accountId);
    return this.http.post<ImportPreviewDTO>(`${this.baseUrl}/import/commit`, formData);
  }

  exportTransactions(filter: Partial<TransactionFilter>): Observable<Blob> {
    const params: Record<string, string> = {};
    Object.entries(filter).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') {
        params[key] = String(value);
      }
    });

    return this.http.get(`${this.baseUrl}/export`, {
      params,
      responseType: 'blob',
    });
  }
}
