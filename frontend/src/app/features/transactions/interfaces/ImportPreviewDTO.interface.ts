import { ImportRowDTO } from './ImportRowDTO.interface';

export interface ImportPreviewDTO {
  rows: ImportRowDTO[];
  totalRows: number;
  validRows: number;
  duplicateRows: number;
  errorRows: number;
  /** Distinct category names the commit will create, from rows that will be imported. */
  newCategories: string[];
}
