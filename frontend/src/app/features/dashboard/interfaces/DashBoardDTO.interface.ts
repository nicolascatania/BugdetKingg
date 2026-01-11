export interface DashBoardDTO {
  totalBalance: number;
  expense: number;
  income: number;
  expensesByCategory: Record<string, number>;
}
