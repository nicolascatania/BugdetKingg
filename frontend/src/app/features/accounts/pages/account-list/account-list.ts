import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-account-list',
  imports: [],
  templateUrl: './account-list.html',
  styleUrl: './account-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccountList { }
