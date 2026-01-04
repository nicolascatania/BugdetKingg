import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { Heading } from "../../components/heading/heading";
import { Accounts } from '../../components/accounts/accounts';
import { MonthlySummary } from "../../components/monthly-summary/monthly-summary";
import { LastMoves } from "../../components/last-moves/last-moves";
import { AccountService } from '../../../accounts/services/AccountService';
import { AccountDTO } from '../../../accounts/interfaces/AccountDTO.interfaces';

@Component({
  selector: 'home',
  imports: [Heading, Accounts, MonthlySummary, LastMoves],
  templateUrl: './home.html',
  styleUrl: './home.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Home {

  accounts = signal<AccountDTO[]>([]);

  constructor(private accountService: AccountService) { }

  ngOnInit() {
    this.accountService.getAccountsByUser().subscribe({
      next: data => this.accounts.set(data),
      error: err => console.error(err)
    });
  }
}
