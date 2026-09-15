import { LastMovesDTO } from '../../transactions/interfaces/LastMovesDTO.interface';

/** Spending on one category: a month in detail plus the all-time total. Only expenses count. */
export interface CategorySpendingDTO {
  categoryId: string;
  categoryName: string;
  categoryIcon: string;
  year: number;
  /** 1-based month number. */
  month: number;
  monthTotal: number;
  monthCount: number;
  allTimeTotal: number;
  allTimeCount: number;
  /** The month's expenses, most recent first. */
  monthTransactions: LastMovesDTO[];
}
