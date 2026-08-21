import { BaseFilter } from '../../../core/interfaces/GenericFilter.interfaces';

export interface BudgetFilter extends BaseFilter {
  year?: number;
  month?: number;
  category?: string;
}
