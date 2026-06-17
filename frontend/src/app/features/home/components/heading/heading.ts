import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  inject,
  Output,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs/operators';
import { AccountService } from '../../../accounts/services/AccountService';
import { DolarService } from '../../../../core/services/dolarService';
import { ArgentinaAPIService } from '../../../../core/services/ArgentinaAPIService';

@Component({
  selector: 'heading',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './heading.html',
  styleUrl: './heading.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Heading {
  private readonly accountService = inject(AccountService);
  private readonly dolarService = inject(DolarService);
  private readonly argentinaAPIService = inject(ArgentinaAPIService);

  readonly dolarCompra = toSignal(
    this.dolarService.getDollarValue().pipe(map((r) => r.compra)),
    { initialValue: 0 },
  );
  readonly dolarVenta = toSignal(
    this.dolarService.getDollarValue().pipe(map((r) => r.venta)),
    { initialValue: 0 },
  );
  readonly inflationValue = toSignal(
    this.argentinaAPIService.getInflation().pipe(map((r) => r.value)),
    { initialValue: 0 },
  );
  readonly inflationDate = toSignal(
    this.argentinaAPIService.getInflation().pipe(map((r) => r.date)),
  );

  readonly totalBalance = this.accountService.totalBalance;

  isLoading = computed(
    () => this.dolarCompra() === 0 || this.inflationValue() === 0,
  );

  @Output() readonly newAccount = new EventEmitter<void>();
  @Output() readonly newTransaction = new EventEmitter<void>();

  openNewAccountModal(): void {
    this.newAccount.emit();
  }
  openNewTransactionModal(): void {
    this.newTransaction.emit();
  }
  get userHasAccounts(): boolean {
    return this.accountService.userHasAccounts();
  }
}
