import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
  OnInit
} from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AccountDTO } from '../../interfaces/AccountDTO.interfaces';
import { AccountService } from '../../services/AccountService';

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
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './edit-account-modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditAccountModal implements OnInit {

  @Input() account: AccountDTO | null = null;
  @Output() closed = new EventEmitter<boolean>();

  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private accountService: AccountService
  ) {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      description: ['', [Validators.maxLength(255), Validators.required]],
      balance: [0, [Validators.required, Validators.min(0)]],
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
      });
    }
  }

  /**
   * Submits the form.
   * The ID is passed through untouched.
   */
  submit(): void {
    if (this.form.invalid) return;

    const payload: AccountDTO = {
      id: this.account?.id ?? null!,
      ...this.form.value,
    };

    const request$ = payload.id
      ? this.accountService.update(payload)
      : this.accountService.create(payload);

    request$.subscribe({
      next: () => this.close(true),
      error: () => alert('Something went wrong. Please try again.'),
    });
  }

  /**
   * Closes the modal.
   */
  close(success = false): void {
    this.closed.emit(success);
  }
}
