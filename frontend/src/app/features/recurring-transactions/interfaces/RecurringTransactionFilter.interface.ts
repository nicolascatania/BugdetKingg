import { BaseFilter } from '../../../core/interfaces/GenericFilter.interfaces';

export interface RecurringTransactionFilter extends BaseFilter {
  description?: string;
  counterparty?: string;
  type?: string;
  frequency?: string;
  account?: string;
  category?: string;
  active?: boolean | '';
  nextRunFrom?: string;
  nextRunTo?: string;
}
