import { Injectable } from '@angular/core';
import { environment } from '../../../../environments/environment.prod';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OptionDTO } from '../../../shared/models/OptionDTO.interface';

@Injectable({
  providedIn: 'root'
})
export class CategoryService {

  private readonly baseUrl = `${environment.apiUrl}/category`;

  constructor(private http: HttpClient) { }


  getOptions(): Observable<OptionDTO[]> {
    return this.http.get<OptionDTO[]>(`${this.baseUrl}/options`);
  }
}
