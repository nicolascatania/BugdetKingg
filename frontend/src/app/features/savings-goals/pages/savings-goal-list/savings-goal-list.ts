import { TranslocoService } from '@jsverse/transloco';
import { TranslocoDirective } from '@jsverse/transloco';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SavingsGoalService } from '../../service/savings-goal-service';
import { SavingsGoalDTO } from '../../interfaces/SavingsGoalDTO.interface';
import { SavingsGoalSummaryDTO } from '../../interfaces/SavingsGoalSummaryDTO.interface';
import { EditSavingsGoal } from '../../components/edit-savings-goal/edit-savings-goal';
import {
  ContributeSavingsGoal,
  ContributionMode,
} from '../../components/contribute-savings-goal/contribute-savings-goal';
import { NotificationService } from '../../../../core/services/NotificationService';
import { PaginationComponent } from '../../../../shared/components/PaginationComponent/PaginationComponent';
import { createPaginationState, PaginationState } from '../../../../core/utils/pagination.util';
import { RevealDirective } from '../../../../shared/directives/reveal.directive';
import { TutorialModal, TutorialSection } from '../../../../shared/components/tutorial-modal/tutorial-modal';

@Component({
  selector: 'app-savings-goal-list',
  standalone: true,
  imports: [CommonModule, EditSavingsGoal, ContributeSavingsGoal, PaginationComponent, RevealDirective, TutorialModal, TranslocoDirective],
  templateUrl: './savings-goal-list.html',
  styleUrl: './savings-goal-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SavingsGoalList implements OnInit {
  private readonly transloco = inject(TranslocoService);

  private savingsGoalService = inject(SavingsGoalService);
  private ns = inject(NotificationService);

  loading = signal(true);
  goals = signal<SavingsGoalDTO[]>([]);
  summary = signal<SavingsGoalSummaryDTO | null>(null);

  paginationState: PaginationState = createPaginationState(12);

  isModalOpen = signal(false);
  selectedGoal = signal<SavingsGoalDTO | null>(null);

  /** Goal and flow of the money modal; `null` mode means it is closed. */
  contributionGoal = signal<SavingsGoalDTO | null>(null);
  contributionMode = signal<ContributionMode | null>(null);

  /** Id of the goal currently being deleted, if any — drives that row's spinner. */
  readonly deletingId = signal<string | null>(null);

  isTutorialOpen = signal(false);
  readonly tutorialSections: TutorialSection[] = [
    { icon: 'fa-bullseye', headingKey: 'savings.tutorial.s1.heading', bodyKey: 'savings.tutorial.s1.body' },
    { icon: 'fa-piggy-bank', headingKey: 'savings.tutorial.s2.heading', bodyKey: 'savings.tutorial.s2.body' },
    { icon: 'fa-chart-line', headingKey: 'savings.tutorial.s3.heading', bodyKey: 'savings.tutorial.s3.body' },
    { icon: 'fa-flag-checkered', headingKey: 'savings.tutorial.s4.heading', bodyKey: 'savings.tutorial.s4.body' },
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

  openContribution(goal: SavingsGoalDTO, mode: ContributionMode): void {
    this.contributionGoal.set(goal);
    this.contributionMode.set(mode);
  }

  onContributionClosed(saved: boolean): void {
    this.contributionGoal.set(null);
    this.contributionMode.set(null);
    if (saved) {
      this.loadGoals();
      this.loadSummary();
    }
  }

  /** Badge primitive per derived state; ACTIVE shows no badge. */
  stateChip(state: SavingsGoalDTO['state']): string {
    switch (state) {
      case 'ACHIEVED':
        return 'chip-positive';
      case 'OVERDUE':
        return 'chip-negative';
      case 'CLOSED':
        return 'chip-neutral';
      default:
        return '';
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
        this.ns.error(err?.error?.message ?? this.transloco.translate('savings.deleteError'));
      },
    });
  }
}
