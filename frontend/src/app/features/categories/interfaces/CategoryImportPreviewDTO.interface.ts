import { CategoryImportRowDTO } from './CategoryImportRowDTO.interface';

export interface CategoryImportPreviewDTO {
  rows: CategoryImportRowDTO[];
  totalRows: number;
  validRows: number;
  duplicateRows: number;
  errorRows: number;
}
