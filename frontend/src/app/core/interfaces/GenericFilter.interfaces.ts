export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // Página actual (empieza en 0)
  last: boolean;
  first: boolean;
}

export interface BaseFilter {
  page: number;
  size: number;
}