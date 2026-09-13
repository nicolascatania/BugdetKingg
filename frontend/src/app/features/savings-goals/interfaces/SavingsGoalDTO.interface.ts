export type SavingsGoalStatus = 'ACTIVE' | 'CLOSED';

/** Derived lifecycle for display; CLOSED mirrors the persisted status. */
export type SavingsGoalState = 'ACTIVE' | 'ACHIEVED' | 'OVERDUE' | 'CLOSED';

export interface SavingsGoalDTO {
  id: string;
  name: string;
  icon: string;
  targetAmount: number;
  targetDate: string;
  /** Default source account preselected when contributing. Does not affect progress. */
  linkedAccountId?: string | null;
  linkedAccountName?: string;
  status: SavingsGoalStatus;
  state: SavingsGoalState;
  achieved: boolean;
  /** Money set aside in the goal. */
  currentAmount: number;
  progressPercentage: number;
  remainingAmount: number;
  monthlyRequired: number;
  daysRemaining: number;
}
