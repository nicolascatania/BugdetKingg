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
  ],
  templateUrl: './recurring-transaction-list.html',
  styleUrl: './recurring-transaction-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RecurringTransactionList implements OnInit {
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
    {
      icon: 'fa-file-pen',
      heading: '1. Create a template',
      body: 'A template is a transaction blueprint: amount, account, category (or destination account for transfers), counterparty and how often it repeats.',
    },
    {
      icon: 'fa-clock',
      heading: '2. Nothing fires by itself — yet',
      body: 'There is no background job running in this app. A template only turns into a real transaction when you press "Run" on it, or when someone calls the run-due endpoint (meant for an external scheduler).',
    },
    {
      icon: 'fa-play',
      heading: '3. Run now vs. next run date',
      body: '"Run" generates one occurrence immediately, regardless of the scheduled date, and advances the template\'s cursor. The "Next run" column shows when it would fire on its own once a scheduler exists.',
    },
    {
      icon: 'fa-toggle-on',
      heading: '4. Pause instead of deleting',
      body: 'Turn a template inactive to stop it from counting as due without losing its configuration — useful for a subscription you paused, for example.',
    },
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
        this.ns.error(err?.error?.message ?? 'Error deleting template');
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
        this.ns.error(err?.error?.message ?? 'Error updating template status');
      },
    });
  }

  runNow(template: RecurringTransactionDTO): void {
    if (this.runningId()) return;

    this.runningId.set(template.id);

    this.recurringService.runNow(template.id).subscribe({
      next: () => {
        this.runningId.set(null);
        this.ns.success('Transaction generated');
        this.search();
        this.loadUpcoming();
      },
      error: (err) => {
        this.runningId.set(null);
        this.ns.error(err?.error?.message ?? 'Error running template');
      },
    });
  }

  get userHasAccounts(): boolean {
    return this.accountService.userHasAccounts();
  }
}
