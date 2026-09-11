export interface AccountImportRowDTO {
  lineNumber: number;
  name: string;
  description: string;
  icon: string;
  valid: boolean;
  errorMessage: string | null;
  duplicate: boolean;
}
