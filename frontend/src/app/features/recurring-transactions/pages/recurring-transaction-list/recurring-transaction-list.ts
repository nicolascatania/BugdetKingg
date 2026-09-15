import { TranslocoService } from '@jsverse/transloco';
import { TranslocoDirective } from '@jsverse/transloco';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { RecurringTransactionService } from '../../service/recurring-transaction-service';
import { RecurringTransactionDTO } from '../../interfaces/RecurringTransactionDTO.interface';
import { RecurringTransactionFilter } from '../../interfaces/RecurringTransactionFilter.interface';
import { RecurrenceFrequency } from '../../interfaces/RecurrenceFrequency.enum';
import { EditRecurringTransaction } from '../../components/edit-recurring-transaction/edit-recurring-transaction';
import { AccountService } from '../../../accounts/services/AccountService';
import { CategoryService } from '../../../categories/service/category-service';
import { OptionDTO } from '../../../../shared/models/OptionDTO.interface';
import { MANUAL_TRANSACTION_TYPES } from '../../../../shared/models/TransactionType.enum';
import { NotificationService } from '../../../../core/services/NotificationService';
import { PaginationComponent } from '../../../../shared/components/PaginationComponent/PaginationComponent';
import { createPaginationState, PaginationState } from '../../../../core/utils/pagination.util';
import { RevealDirective } from '../../../../shared/directives/reveal.directive';
import { TutorialModal, TutorialSection } from '../../../../shared/components/tutorial-modal/tutorial-modal';

@Component({
  selector: 'app-recurring-transaction-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    EditRecurringTransaction,
    PaginationComponent,
    RevealDirective,
    TutorialModal,
    TranslocoDirective,
  ],
  templateUrl: './recurring-transaction-list.html',
  styleUrl: './recurring-transaction-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RecurringTransactionList implements OnInit {
  private readonly transloco = inject(TranslocoService);

  private recurringService = inject(RecurringTransactionService);
  private accountService = inject(AccountService);
  private categoryService = inject(CategoryService);
  private ns = inject(NotificationService);
  private fb = inject(FormBuilder);

  /** Starts true so the first paint shows placeholders, never a flash of the empty state. */
  loading = signal(true);
  templates = signal<RecurringTransactionDTO[]>([]);
  upcoming = signal<RecurringTransactionDTO[]>([]);

  accounts = signal<OptionDTO[]>([]);
  categories = signal<OptionDTO[]>([]);
  frequencies = Object.values(RecurrenceFrequency);
  transactionTypes = MANUAL_TRANSACTION_TYPES;

  paginationState: PaginationState = createPaginationState(20);
  form: FormGroup;

  isModalOpen = signal(false);
  selectedTemplate = signal<RecurringTransactionDTO | null>(null);

  /** Id of the template currently being deleted, if any — drives that row's spinner. */
  readonly deletingId = signal<string | null>(null);

  /** Id of the template currently being run, if any — drives that row's spinner. */
  readonly runningId = signal<string | null>(null);

  isTutorialOpen = signal(false);
  readonly tutorialSections: TutorialSection[] = [
    { icon: 'fa-file-pen', headingKey: 'recurring.tutorial.s1.heading', bodyKey: 'recurring.tutorial.s1.body' },
    { icon: 'fa-clock', headingKey: 'recurring.tutorial.s2.heading', bodyKey: 'recurring.tutorial.s2.body' },
    { icon: 'fa-play', headingKey: 'recurring.tutorial.s3.heading', bodyKey: 'recurring.tutorial.s3.body' },
    { icon: 'fa-toggle-on', headingKey: 'recurring.tutorial.s4.heading', bodyKey: 'recurring.tutorial.s4.body' },
  ];

  constructor() {
    this.form = this.fb.group({
      description: [''],
      counterparty: [''],
      account: [''],
      category: [''],
      type: [''],
      frequency: [''],
      active: [''],
    });
  }

  ngOnInit(): void {
    forkJoin({
      accounts: this.accountService.getOptions(),
      categories: this.categoryService.getOptions(),
    }).subscribe({
      next: ({ accounts, categories }) => {
        this.accounts.set(accounts);
        this.categories.set(categories);
        this.search();
      },
      error: (err) => this.ns.error(err),
    });

    this.loadUpcoming();
  }

  search(): void {
    this.loading.set(true);
    const filter: RecurringTransactionFilter = {
      page: this.paginationState.currentPage(),
      size: this.paginationState.pageSize(),
      ...this.form.value,
      active: this.form.value.active === '' ? undefined : this.form.value.active === 'true',
    };

    this.recurringService.search(filter).subscribe({
      next: (data) => {
        this.templates.set(data.content);
        this.paginationState.updateFromResponse(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.ns.error(err);
        this.templates.set([]);
        this.loading.set(false);
      },
    });
  }

  private loadUpcoming(): void {
    this.recurringService.getUpcoming().subscribe({
      next: (data) => this.upcoming.set(data.slice(0, 6)),
      error: () => this.upcoming.set([]),
    });
  }

  onClear(): void {
    this.form.reset({
      description: '',
      counterparty: '',
      account: '',
      category: '',
      type: '',
      frequency: '',
      active: '',
    });
    this.search();
  }

  onPageChange(page: number): void {
    this.paginationState.goToPage(page);
    this.search();
  }

  openNewModal(): void {
    this.selectedTemplate.set(null);
    this.isModalOpen.set(true);
  }

  openEditModal(template: RecurringTransactionDTO): void {
    this.selectedTemplate.set(template);
    this.isModalOpen.set(true);
  }

  onModalClosed(saved: boolean): void {
    this.isModalOpen.set(false);
    if (saved) {
      this.search();
      this.loadUpcoming();
    }
  }

  deleteTemplate(template: RecurringTransactionDTO): void {
    if (this.deletingId()) return;

    this.deletingId.set(template.id);

    this.recurringService.delete(template.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.search();
        this.loadUpcoming();
      },
      error: (err) => {
        this.deletingId.set(null);
        this.ns.error(err?.error?.message ?? this.transloco.translate('recurring.deleteError'));
      },
    });
  }

  /** Flips active/paused in a single click, updating the row immediately and rolling back on error. */
  toggleActive(template: RecurringTransactionDTO): void {
    const nextActive = !template.active;

    this.templates.update((list) =>
      list.map((t) => (t.id === template.id ? { ...t, active: nextActive } : t)),
    );

    this.recurringService.update({ ...template, active: nextActive }).subscribe({
      next: () => this.loadUpcoming(),
      error: (err) => {
        this.templates.update((list) =>
          list.map((t) => (t.id === template.id ? { ...t, active: template.active } : t)),
        );
        this.ns.error(err?.error?.message ?? this.transloco.translate('recurring.statusError'));
      },
    });
  }

  runNow(template: RecurringTransactionDTO): void {
    if (this.runningId()) return;

    this.runningId.set(template.id);

    this.recurringService.runNow(template.id).subscribe({
      next: () => {
        this.runningId.set(null);
        this.ns.success(this.transloco.translate('recurring.generated'));
        this.search();
        this.loadUpcoming();
      },
      error: (err) => {
        this.runningId.set(null);
        this.ns.error(err?.error?.message ?? this.transloco.translate('recurring.runError'));
      },
    });
  }

  get userHasAccounts(): boolean {
    return this.accountService.userHasAccounts();
  }
}
