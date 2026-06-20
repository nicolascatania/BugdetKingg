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
import { AccountService } from '../../../accounts/services/AccountService';
import { DolarService } from '../../../../core/services/dolarService';
import { ArgentinaAPIService } from '../../../../core/services/ArgentinaAPIService';
import { InflationResponseDTO } from '../../../../core/interfaces/Client.interfaces';

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

  private readonly dolarData = toSignal(this.dolarService.getDollarValue(), {
    initialValue: { compra: 0, venta: 0 },
  });

  private readonly inflationData = toSignal(
    this.argentinaAPIService.getInflation(),
    {
      initialValue: { value: 0, date: new Date(0) } as InflationResponseDTO,
    },
  );
  readonly dolarCompra = computed(() => this.dolarData()?.compra ?? 0);
  readonly dolarVenta = computed(() => this.dolarData()?.venta ?? 0);
  readonly inflationValue = computed(() => this.inflationData()?.value ?? 0);
  readonly inflationDate = computed(() => this.inflationData()?.date ?? '');

  readonly totalBalance = this.accountService.totalBalance;

  // isLoading solo es true si los datos críticos siguen en su valor inicial (0)
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
