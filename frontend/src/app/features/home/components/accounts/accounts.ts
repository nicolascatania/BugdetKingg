import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { AccountDTO } from '../../../accounts/interfaces/AccountDTO.interfaces';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'accounts',
  imports: [CommonModule],
  templateUrl: './accounts.html',
  styleUrl: './accounts.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Accounts { 

  accounts = input<AccountDTO[]>([]);


  constructor() {}

  ngAfterViewInit() {
    console.log(this.accounts);
  }

}
