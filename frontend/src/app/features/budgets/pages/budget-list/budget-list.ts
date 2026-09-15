import { LOCALE_ID } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';
import { TranslocoDirective } from '@jsverse/transloco';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { BudgetService } from '../../service/budget-service';
import { BudgetProgressDTO } from '../../interfaces/BudgetProgressDTO.interface';
import { BudgetDTO } from '../../interfaces/BudgetDTO.interface';
import { EditBudget } from '../../components/edit-budget/edit-budget';
import { NotificationService } from '../../../../core/services/NotificationService';
import { RevealDirective } from '../../../../shared/directives/reveal.directive';
import { TutorialModal, TutorialSection } from '../../../../shared/components/tutorial-modal/tutorial-modal';
import { monthOptions } from '../../../../shared/models/months.const';

@Component({
  selector: 'app-budget-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, EditBudget, RevealDirective, TutorialModal, TranslocoDirective],
  templateUrl: './budget-list.html',
  styleUrl: './budget-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BudgetList implements OnInit {
  private readonly transloco = inject(TranslocoService);

  private budgetService = inject(BudgetService);
  private ns = inject(NotificationService);
  private fb = inject(FormBuilder);

  loading = signal(true);
  progress = signal<BudgetProgressDTO[]>([]);

  isModalOpen = signal(false);
  selectedBudget = signal<BudgetDTO | null>(null);

  /** Id of the budget currently being deleted, if any — drives that row's spinner. */
  readonly deletingId = signal<BudgetProgressDTO['budgetId'] | null>(null);

  isTutorialOpen = signal(false);
  readonly tutorialSections: TutorialSection[] = [
    { icon: 'fa-tags', headingKey: 'budgets.tutorial.s1.heading', bodyKey: 'budgets.tutorial.s1.body' },
    { icon: 'fa-chart-pie', headingKey: 'budgets.tutorial.s2.heading', bodyKey: 'budgets.tutorial.s2.body' },
    { icon: 'fa-triangle-exclamation', headingKey: 'budgets.tutorial.s3.heading', bodyKey: 'budgets.tutorial.s3.body' },
    { icon: 'fa-calendar', headingKey: 'budgets.tutorial.s4.heading', bodyKey: 'budgets.tutorial.s4.body' },
  ];

  readonly years = this.buildYearRange();
  readonly months = monthOptions(inject(LOCALE_ID));

  periodForm: FormGroup = this.fb.group({
    year: [new Date().getFullYear()],
    month: [new Date().getMonth() + 1],
  });

  ngOnInit(): void {
    this.periodForm.valueChanges.subscribe(() => this.loadProgress());
    this.loadProgress();
  }

  private loadProgress(): void {
    this.loading.set(true);
    const { year, month } = this.periodForm.value;

    this.budgetService.getProgress(year, month).subscribe({
      next: (data) => {
        this.progress.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.ns.error(err);
        this.progress.set([]);
        this.loading.set(false);
      },
    });
  }

  openNewBudgetModal(): void {
    this.selectedBudget.set(null);
    this.isModalOpen.set(true);
  }

  openEditBudgetModal(p: BudgetProgressDTO): void {
    const { year, month } = this.periodForm.value;
    this.selectedBudget.set({
      id: p.budgetId,
      category: p.categoryId,
      categoryName: p.categoryName,
      categoryIcon: p.categoryIcon,
      year,
      month,
      limitAmount: p.limitAmount,
    });
    this.isModalOpen.set(true);
  }

  deleteBudget(p: BudgetProgressDTO): void {
    if (this.deletingId()) return;

    this.deletingId.set(p.budgetId);

    this.budgetService.delete(p.budgetId).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.loadProgress();
      },
      error: (err) => {
        this.deletingId.set(null);
        this.ns.error(err?.error?.message ?? this.transloco.translate('budgets.deleteError'));
      },
    });
  }

  onModalClosed(saved: boolean): void {
    this.isModalOpen.set(false);
    if (saved) this.loadProgress();
  }

  statusChip(status: string): string {
    switch (status) {
      case 'EXCEEDED':
        return 'chip-negative';
      case 'WARNING':
        return 'chip-neutral';
      default:
        return 'chip-positive';
    }
  }

  barColor(status: string): string {
    switch (status) {
      case 'EXCEEDED':
        return 'bg-negative';
      case 'WARNING':
        return 'bg-warning';
      default:
        return 'bg-positive';
    }
  }

  private buildYearRange(): number[] {
    const current = new Date().getFullYear();
    return Array.from({ length: 6 }, (_, i) => current - 2 + i);
  }
}
