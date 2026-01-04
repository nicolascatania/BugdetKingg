import { Injectable } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AccountDTO } from '../interfaces/AccountDTO.interfaces';

@Injectable({
  providedIn: 'root'
})
export class AccountService {


  private readonly baseUrl = `${environment.apiUrl}/account`;

  constructor(private HttpClient: HttpClient) { }


  getAccountsByUser(): Observable<AccountDTO[]> {
    return this.HttpClient.get<AccountDTO[]>(`${this.baseUrl}/by-user`);
  }

}

