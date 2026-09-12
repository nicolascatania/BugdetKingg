import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SavingsGoalService } from '../../service/savings-goal-service';
import { SavingsGoalDTO } from '../../interfaces/SavingsGoalDTO.interface';
import { SavingsGoalSummaryDTO } from '../../interfaces/SavingsGoalSummaryDTO.interface';
import { EditSavingsGoal } from '../../components/edit-savings-goal/edit-savings-goal';
import { NotificationService } from '../../../../core/services/NotificationService';
import { PaginationComponent } from '../../../../shared/components/PaginationComponent/PaginationComponent';
import { createPaginationState, PaginationState } from '../../../../core/utils/pagination.util';
import { RevealDirective } from '../../../../shared/directives/reveal.directive';
import { TutorialModal, TutorialSection } from '../../../../shared/components/tutorial-modal/tutorial-modal';

@Component({
  selector: 'app-savings-goal-list',
  standalone: true,
  imports: [CommonModule, EditSavingsGoal, PaginationComponent, RevealDirective, TutorialModal],
  templateUrl: './savings-goal-list.html',
  styleUrl: './savings-goal-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SavingsGoalList implements OnInit {
  private savingsGoalService = inject(SavingsGoalService);
  private ns = inject(NotificationService);

  loading = signal(true);
  goals = signal<SavingsGoalDTO[]>([]);
  summary = signal<SavingsGoalSummaryDTO | null>(null);

  paginationState: PaginationState = createPaginationState(12);

  isModalOpen = signal(false);
  selectedGoal = signal<SavingsGoalDTO | null>(null);

  /** Id of the goal currently being deleted, if any — drives that row's spinner. */
  readonly deletingId = signal<string | null>(null);

  isTutorialOpen = signal(false);
  readonly tutorialSections: TutorialSection[] = [
    {
      icon: 'fa-bullseye',
      heading: '1. Name a target',
      body: 'Give the goal a name, an icon, how much you need to reach it, and the date you want to reach it by.',
    },
    {
      icon: 'fa-link',
      heading: '2. Link an account (optional)',
      body: 'Linking an account makes the goal track that account\'s balance automatically as "current amount" — no manual updates needed. Leave it unlinked if you just want to plan a number.',
    },
    {
      icon: 'fa-chart-line',
      heading: '3. Everything else is calculated for you',
      body: 'Progress %, remaining amount, monthly amount required and days left are all derived from the linked account and the target date — you never edit them directly.',
    },
    {
      icon: 'fa-flag-checkered',
      heading: '4. Achieved goals',
      body: 'Once the linked account balance reaches the target, the goal is marked "Achieved" automatically.',
    },
  ];

  ngOnInit(): void {
    this.loadSummary();
    this.loadGoals();
  }

  private loadSummary(): void {
    this.savingsGoalService.getSummary().subscribe({
      next: (data) => this.summary.set(data),
      error: (err) => this.ns.error(err),
    });
  }

  private loadGoals(): void {
    this.loading.set(true);
    this.savingsGoalService.search(this.paginationState.getFilter()).subscribe({
      next: (data) => {
        this.goals.set(data.content);
        this.paginationState.updateFromResponse(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.ns.error(err);
        this.goals.set([]);
        this.loading.set(false);
      },
    });
  }

  onPageChange(page: number): void {
    this.paginationState.goToPage(page);
    this.loadGoals();
  }

  openNewModal(): void {
    this.selectedGoal.set(null);
    this.isModalOpen.set(true);
  }

  openEditModal(goal: SavingsGoalDTO): void {
    this.selectedGoal.set(goal);
    this.isModalOpen.set(true);
  }

  onModalClosed(saved: boolean): void {
    this.isModalOpen.set(false);
    if (saved) {
      this.loadGoals();
      this.loadSummary();
    }
  }

  deleteGoal(goal: SavingsGoalDTO): void {
    if (this.deletingId()) return;

    this.deletingId.set(goal.id);

    this.savingsGoalService.delete(goal.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.loadGoals();
        this.loadSummary();
      },
      error: (err) => {
        this.deletingId.set(null);
        this.ns.error(err?.error?.message ?? 'Error deleting savings goal');
      },
    });
  }
}
