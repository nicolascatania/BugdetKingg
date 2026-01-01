import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Heading } from "../../components/heading/heading";
import { Accounts } from '../../components/accounts/accounts';
import { MonthlySummary } from "../../components/monthly-summary/monthly-summary";
import { LastMoves } from "../../components/last-moves/last-moves";

@Component({
  selector: 'home',
  imports: [Heading, Accounts, MonthlySummary, LastMoves],
  templateUrl: './home.html',
  styleUrl: './home.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Home { }
