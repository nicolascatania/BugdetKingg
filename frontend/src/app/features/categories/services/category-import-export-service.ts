import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { CategoryImportPreviewDTO } from '../interfaces/CategoryImportPreviewDTO.interface';

@Injectable({
  providedIn: 'root',
})
export class CategoryImportExportService {
  private readonly baseUrl = `${environment.apiUrl}/category`;

  constructor(private http: HttpClient) {}

  previewImport(file: File): Observable<CategoryImportPreviewDTO> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<CategoryImportPreviewDTO>(`${this.baseUrl}/import/preview`, formData);
  }

  commitImport(file: File): Observable<CategoryImportPreviewDTO> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<CategoryImportPreviewDTO>(`${this.baseUrl}/import/commit`, formData);
  }

  exportCategories(): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/export`, { responseType: 'blob' });
  }
}
