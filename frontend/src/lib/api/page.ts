/** One page of a paged list, as returned by the API. */
export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
}
