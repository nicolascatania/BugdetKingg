import { ImportRowDTO } from './ImportRowDTO.interface';

export interface ImportPreviewDTO {
  rows: ImportRowDTO[];
  totalRows: number;
  validRows: number;
  duplicateRows: number;
  errorRows: number;
}
