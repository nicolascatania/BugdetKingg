export interface CategoryExpense {
  name: string;
  icon: string;
  amount: number;
  percentage: number;
}

export interface DashBoardDTO {
  totalBalance: number;
  expense: number;
  income: number;
  /** `income - expense` for the selected range; negative when more went out than came in. */
  netBalance: number;
  expensesByCategory: CategoryExpense[];
}
