import { BaseFilter } from '../../../core/interfaces/GenericFilter.interfaces';
import { SavingsGoalStatus } from './SavingsGoalDTO.interface';

export interface SavingsGoalFilter extends BaseFilter {
  name?: string;
  achieved?: boolean | '';
  status?: SavingsGoalStatus | '';
  targetAmountMin?: number;
  targetAmountMax?: number;
}
