import { isOutflowType, TransactionType } from '../models/TransactionType.enum';

/**
 * Presentation rules shared by every list that renders a transaction type, so a
 * new type only has to be taught here. Savings movements are neutral: money
 * changes pocket, it is neither earned nor spent.
 */

/** Chip primitive for the type badge. */
export function transactionTypeChip(type: string | null | undefined): string {
  switch (type) {
    case TransactionType.INCOME:
      return 'chip-positive';
    case TransactionType.EXPENSE:
      return 'chip-negative';
    case TransactionType.SAVINGS_DEPOSIT:
    case TransactionType.SAVINGS_WITHDRAWAL:
      return 'chip-brand';
    default:
      return 'chip-neutral';
  }
}

/** Short, human label for the badge. */
export function transactionTypeLabel(type: string | null | undefined): string {
  switch (type) {
    case TransactionType.SAVINGS_DEPOSIT:
      return 'TO SAVINGS';
    case TransactionType.SAVINGS_WITHDRAWAL:
      return 'FROM SAVINGS';
    default:
      return type ?? '';
  }
}

/** Text color for the amount figure; transfers and savings stay neutral. */
export function transactionAmountClass(type: string | null | undefined): string {
  if (type === TransactionType.INCOME) return 'text-positive';
  if (type === TransactionType.EXPENSE) return 'text-negative';
  return '';
}

/** Leading sign for the amount figure: anything leaving the account is negative. */
export function transactionAmountSign(type: string | null | undefined): string {
  return isOutflowType(type) ? '-' : '';
}
