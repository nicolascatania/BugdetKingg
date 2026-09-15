export type BudgetStatus = 'OK' | 'WARNING' | 'EXCEEDED';

export interface BudgetProgressDTO {
  budgetId: string;
  categoryId: string;
  categoryName: string;
  categoryIcon: string;
  /** Period the budget row itself was created for; earlier than the selected one when a recurring budget carries over. */
  year: number;
  month: number;
  recurring: boolean;
  limitAmount: number;
  spentAmount: number;
  remainingAmount: number;
  percentage: number;
  status: BudgetStatus;
}
