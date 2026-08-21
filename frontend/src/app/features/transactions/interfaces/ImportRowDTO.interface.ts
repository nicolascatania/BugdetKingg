export interface ImportRowDTO {
  lineNumber: number;
  date: string;
  description: string;
  amount: number | null;
  type: string;
  category: string;
  counterparty: string;
  account: string | null;
  valid: boolean;
  errorMessage: string | null;
  duplicate: boolean;
}
