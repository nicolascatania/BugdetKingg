export interface BudgetDTO {
  id: string;
  category: string;
  categoryName?: string;
  categoryIcon?: string;
  year: number;
  month: number;
  limitAmount: number;
}
