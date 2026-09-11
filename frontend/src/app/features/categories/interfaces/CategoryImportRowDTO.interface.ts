export interface CategoryImportRowDTO {
  lineNumber: number;
  name: string;
  icon: string;
  valid: boolean;
  errorMessage: string | null;
  duplicate: boolean;
}
