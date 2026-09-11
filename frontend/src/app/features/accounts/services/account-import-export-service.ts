import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AccountImportPreviewDTO } from '../interfaces/AccountImportPreviewDTO.interface';

@Injectable({
  providedIn: 'root',
})
export class AccountImportExportService {
  private readonly baseUrl = `${environment.apiUrl}/account`;

  constructor(private http: HttpClient) {}

  previewImport(file: File): Observable<AccountImportPreviewDTO> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<AccountImportPreviewDTO>(`${this.baseUrl}/import/preview`, formData);
  }

  commitImport(file: File): Observable<AccountImportPreviewDTO> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<AccountImportPreviewDTO>(`${this.baseUrl}/import/commit`, formData);
  }

  exportAccounts(): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/export`, { responseType: 'blob' });
  }
}
