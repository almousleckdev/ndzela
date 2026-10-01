import {
  columnVisibilityFeature,
  createColumnHelper,
  createPaginatedRowModel,
  createSortedRowModel,
  rowPaginationFeature,
  rowSortingFeature,
  tableFeatures,
  type ColumnDef,
  type RowData,
} from "@tanstack/react-table";

/** One feature set for every table: sorting, pagination and column visibility. Server tables switch the row models off with the manual flags. */
export const dataTableFeatures = tableFeatures({
  rowSortingFeature,
  sortedRowModel: createSortedRowModel(),
  rowPaginationFeature,
  paginatedRowModel: createPaginatedRowModel(),
  columnVisibilityFeature,
});

export type Col<T extends RowData> = ColumnDef<typeof dataTableFeatures, T>;

export const columnsFor = <T extends RowData>() => createColumnHelper<typeof dataTableFeatures, T>();
