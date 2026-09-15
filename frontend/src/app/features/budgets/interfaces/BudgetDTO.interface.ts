export interface BudgetDTO {
  id: string;
  category: string;
  categoryName?: string;
  categoryIcon?: string;
  year: number;
  month: number;
  limitAmount: number;
  /** The limit repeats every month from `year`/`month` until a later budget for the category takes over. */
  recurring: boolean;
}
