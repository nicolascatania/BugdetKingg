export interface PageResponse<T> {
  content: T[];
  page: {
    size: number;
    totalElements: number;
    totalPages: number;
    number: number;
  };
}
export interface BaseFilter {
  page: number;
  size: number;
}