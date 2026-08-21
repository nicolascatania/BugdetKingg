export interface SavingsGoalDTO {
  id: string;
  name: string;
  icon: string;
  targetAmount: number;
  targetDate: string;
  linkedAccountId?: string | null;
  linkedAccountName?: string;
  achieved: boolean;
  currentAmount: number;
  progressPercentage: number;
  remainingAmount: number;
  monthlyRequired: number;
  daysRemaining: number;
}
