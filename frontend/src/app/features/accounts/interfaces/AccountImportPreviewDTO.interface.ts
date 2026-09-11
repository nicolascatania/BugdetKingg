import { AccountImportRowDTO } from './AccountImportRowDTO.interface';

export interface AccountImportPreviewDTO {
  rows: AccountImportRowDTO[];
  totalRows: number;
  validRows: number;
  duplicateRows: number;
  errorRows: number;
}
