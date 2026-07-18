import { Injectable } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BaseService } from '../../../core/services/BaseService';
import {
  AppUserDTO,
  AppUserForListDTO,
} from '../interfaces/AppUserDTO.interface';
import { PageResponse } from '../../../core/interfaces/GenericFilter.interfaces';

@Injectable({
  providedIn: 'root',
})
export class UserService extends BaseService<AppUserForListDTO> {
  protected readonly baseUrl = `${environment.apiUrl}/users`;

  constructor(http: HttpClient) {
    super(http);
  }

  save(user: AppUserForListDTO): Observable<AppUserForListDTO> {
    if (user.id) {
      return this.update(user);
    } else {
      return this.create(user);
    }
  }

  getListForWebsite(
    page: number,
    size: number,
  ): Observable<PageResponse<AppUserForListDTO>> {
    return this.http.get<PageResponse<AppUserForListDTO>>(
      `${this.baseUrl}?page=${page}&size=${size}`,
    );
  }
}
