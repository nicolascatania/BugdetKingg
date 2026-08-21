export type BudgetStatus = 'OK' | 'WARNING' | 'EXCEEDED';

export interface BudgetProgressDTO {
  budgetId: string;
  categoryId: string;
  categoryName: string;
  categoryIcon: string;
  limitAmount: number;
  spentAmount: number;
  remainingAmount: number;
  percentage: number;
  status: BudgetStatus;
}
