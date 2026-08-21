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
import { MONTHS } from '../../../../shared/models/months.const';

@Component({
  selector: 'app-budget-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, EditBudget, RevealDirective, TutorialModal],
  templateUrl: './budget-list.html',
  styleUrl: './budget-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BudgetList implements OnInit {
  private budgetService = inject(BudgetService);
  private ns = inject(NotificationService);
  private fb = inject(FormBuilder);

  loading = signal(true);
  progress = signal<BudgetProgressDTO[]>([]);

  isModalOpen = signal(false);
  selectedBudget = signal<BudgetDTO | null>(null);

  isTutorialOpen = signal(false);
  readonly tutorialSections: TutorialSection[] = [
    {
      icon: 'fa-tags',
      heading: '1. Set a limit per category',
      body: 'Pick a category, a month and year, and how much you want to spend at most on it during that period.',
    },
    {
      icon: 'fa-chart-pie',
      heading: '2. Watch it fill up automatically',
      body: 'Every expense you log against that category counts toward the limit — there is nothing else to do. The progress bar and the spent/remaining numbers update on their own.',
    },
    {
      icon: 'fa-triangle-exclamation',
      heading: '3. Colors tell you the status',
      body: 'Green (OK) while you are under 80% spent, amber (WARNING) from 80% up, red (EXCEEDED) once you go over the limit.',
    },
    {
      icon: 'fa-calendar',
      heading: '4. Budgets are per month',
      body: 'Switch the month/year selector at the top to review a past period or set up next month\'s limits ahead of time.',
    },
  ];

  readonly years = this.buildYearRange();
  readonly months = MONTHS;

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
    this.budgetService.delete(p.budgetId).subscribe({
      next: () => this.loadProgress(),
      error: (err) => this.ns.error(err?.error?.message ?? 'Error deleting budget'),
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
