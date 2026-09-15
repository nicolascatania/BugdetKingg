import { inject } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';
import { TranslocoDirective } from '@jsverse/transloco';
import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
  OnInit,
  signal,
} from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AccountDTO } from '../../interfaces/AccountDTO.interfaces';
import { AccountService } from '../../services/AccountService';
import { UiModalComponent } from '../../../../shared/modal/ui-modal/ui-modal';
import { FINANCIAL_ICONS } from '../../../icons/interfaces/iconsenum.interace';

/**
 * Modal used to create or edit an account.
 *
 * Rules:
 * - If account.id exists → PUT /account/{id}
 * - If account.id is null → POST /account
 *
 * The ID is never exposed nor modified by the UI.
 */
@Component({
  selector: 'app-edit-account-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, UiModalComponent, TranslocoDirective],
  templateUrl: './edit-account-modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditAccountModal implements OnInit {
  private readonly transloco = inject(TranslocoService);

  @Input() account: AccountDTO | null = null;
  @Output() closed = new EventEmitter<boolean>();

  form: FormGroup;

  /** Disables the form and shows progress while the request is in flight. */
  readonly saving = signal(false);

  protected iconOptions = FINANCIAL_ICONS;

  constructor(
    private fb: FormBuilder,
    private accountService: AccountService,
  ) {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      description: ['', [Validators.maxLength(255), Validators.required]],
      // balance: [0, [Validators.required, Validators.min(0)]], NOT USED FOR NOW, USER SETS A NEW ACCOUNT AND STARTS DOING TXS, NOT SETTING THE ACTUAL AMMOUNT
      icon: ['fa-wallet', [Validators.required]],
    });
  }

  /**
   * Initializes the form when editing an existing account.
   */
  ngOnInit(): void {
    if (this.account) {
      this.form.patchValue({
        name: this.account.name,
        description: this.account.description,
        balance: this.account.balance,
        icon: this.account.icon || 'fa-wallet',
      });
    }
  }

  /**
   * Submits the form.
   * The ID is passed through untouched.
   */
  submit(): void {
    if (this.form.invalid || this.saving()) return;

    const payload: AccountDTO = {
      id: this.account?.id ?? null!,
      ...this.form.value,
    };

    const request$ = payload.id
      ? this.accountService.update(payload)
      : this.accountService.create(payload);

    this.saving.set(true);

    request$.subscribe({
      next: () => this.close(true),
      error: () => {
        this.saving.set(false);
        alert(this.transloco.translate('accounts.form.saveError'));
      },
    });
  }

  /**
   * Closes the modal.
   */
  close(success = false): void {
    this.closed.emit(success);
  }
}
