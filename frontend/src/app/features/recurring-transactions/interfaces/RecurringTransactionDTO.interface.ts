export interface RecurringTransactionDTO {
  id: string;
  description: string;
  amount: number;
  type: string;
  counterparty: string;
  category?: string | null;
  categoryName?: string;
  account: string;
  accountName?: string;
  destinationAccount?: string | null;
  frequency: string;
  nextRunDate: string;
  endDate?: string | null;
  active: boolean;
}
