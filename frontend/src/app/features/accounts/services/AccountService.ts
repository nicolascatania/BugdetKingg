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

  create(account: AccountDTO): Observable<AccountDTO> {
    return this.HttpClient.post<AccountDTO>(this.baseUrl, account);
  }

  update(account: AccountDTO): Observable<AccountDTO> {
    return this.HttpClient.put<AccountDTO>(`${this.baseUrl}/${account.id}`, account);
  }

  delete(accountId: string): Observable<void> {
    return this.HttpClient.delete<void>(`${this.baseUrl}/${accountId}`);
  }

}

