import { Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { TransactionDTO } from '../interfaces/TransactionDTO.interface';
import { HttpClient } from '@angular/common/http';
import { MonthlyTransactionReportDTO } from '../interfaces/MonthlyTransactionReportDTO.interface';
import { LastMovesDTO } from '../interfaces/LastMovesDTO.interface';


@Injectable({
  providedIn: 'root'
})
export class TransactionService {

  private readonly baseUrl = `${environment.apiUrl}/transaction`;


  private refreshSignal = signal(0);

  refresh$ = this.refreshSignal.asReadonly();

  notifyRefresh() {
    this.refreshSignal.update(v => v + 1);
  }


  constructor(private HttpClient: HttpClient) { }


  getTransactionsByUser(): Observable<TransactionDTO[]> {
    return this.HttpClient.get<TransactionDTO[]>(`${this.baseUrl}/by-user`);
  }

  getMovementsOfThisMonth(): Observable<LastMovesDTO[]> {
    return this.HttpClient.get<LastMovesDTO[]>(`${this.baseUrl}/movements-this-month`);
  }

  getCurrentMonthlyReport(): Observable<MonthlyTransactionReportDTO> {
    return this.HttpClient.get<MonthlyTransactionReportDTO>(`${this.baseUrl}/monthly-balance`);
  }

  create(account: TransactionDTO): Observable<TransactionDTO> {
    return this.HttpClient.post<TransactionDTO>(this.baseUrl, account)
      .pipe(
        tap(() => this.notifyRefresh())
      );
  }

  update(account: TransactionDTO): Observable<TransactionDTO> {
    return this.HttpClient.put<TransactionDTO>(`${this.baseUrl}/${account.id}`, account)
      .pipe(
        tap(() => this.notifyRefresh())
      );
  }

  delete(accountId: string): Observable<void> {
    return this.HttpClient.delete<void>(`${this.baseUrl}/${accountId}`)
      .pipe(
        tap(() => this.notifyRefresh())
      );
  }

}
