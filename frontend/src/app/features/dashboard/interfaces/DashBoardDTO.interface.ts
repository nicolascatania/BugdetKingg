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
  expensesByCategory: CategoryExpense[];
}
