export enum TransactionType {
  INCOME = 'INCOME',
  EXPENSE = 'EXPENSE',
  TRANSFER = 'TRANSFER',
  /** Account → savings goal. Only created from the savings goals screen. */
  SAVINGS_DEPOSIT = 'SAVINGS_DEPOSIT',
  /** Savings goal → account. Only created from the savings goals screen. */
  SAVINGS_WITHDRAWAL = 'SAVINGS_WITHDRAWAL',
}

/** Types the user can pick when logging a transaction by hand. */
export const MANUAL_TRANSACTION_TYPES = [
  TransactionType.EXPENSE,
  TransactionType.INCOME,
  TransactionType.TRANSFER,
] as const;

/** Whether a type moves money to or from a savings goal. */
export function isSavingsType(type: string | null | undefined): boolean {
  return type === TransactionType.SAVINGS_DEPOSIT || type === TransactionType.SAVINGS_WITHDRAWAL;
}

/** Whether a type takes money out of the account it is logged on. */
export function isOutflowType(type: string | null | undefined): boolean {
  return type === TransactionType.EXPENSE || type === TransactionType.SAVINGS_DEPOSIT;
}
