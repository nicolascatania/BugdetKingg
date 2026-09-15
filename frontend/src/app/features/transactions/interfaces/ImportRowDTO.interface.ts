export interface ImportRowDTO {
  lineNumber: number;
  date: string;
  description: string;
  amount: number | null;
  type: string;
  category: string;
  /** The category does not exist yet; committing creates it with the default icon. */
  newCategory: boolean;
  counterparty: string;
  account: string | null;
  destinationAccount: string | null;
  valid: boolean;
  errorMessage: string | null;
  duplicate: boolean;
}
