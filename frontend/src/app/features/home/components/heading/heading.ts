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
import { catchError, of } from 'rxjs';
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

  /**
   * Both feeds start as `null` rather than as a zero-filled object, so "not
   * answered yet" stays distinguishable from "answered with 0". A failing
   * external API resolves to `null` too, which dismisses the placeholders
   * instead of leaving them shimmering forever.
   */
  private readonly dolarData = toSignal(
    this.dolarService.getDollarValue().pipe(catchError(() => of(null))),
    { initialValue: undefined },
  );

  private readonly inflationData = toSignal(
    this.argentinaAPIService
      .getInflation()
      .pipe(catchError(() => of(null as InflationResponseDTO | null))),
    { initialValue: undefined },
  );

  readonly dolarCompra = computed(() => this.dolarData()?.compra ?? 0);
  readonly dolarVenta = computed(() => this.dolarData()?.venta ?? 0);
  readonly inflationValue = computed(() => this.inflationData()?.value ?? 0);
  readonly inflationDate = computed(() => this.inflationData()?.date ?? '');

  readonly totalBalance = this.accountService.totalBalance;

  /** Placeholders stay up only while a feed has not settled yet. */
  isLoading = computed(
    () => this.dolarData() === undefined || this.inflationData() === undefined,
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
