import { BaseFilter } from '../../../core/interfaces/GenericFilter.interfaces';

export interface SavingsGoalFilter extends BaseFilter {
  name?: string;
  achieved?: boolean | '';
  targetAmountMin?: number;
  targetAmountMax?: number;
}
