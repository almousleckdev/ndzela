"use client";

import { useCallback, useState } from "react";
import type { SortingState } from "@tanstack/react-table";
import { useDebounced } from "@/hooks/use-debounced";
import { NO_RANGE, type DateRange } from "@/lib/date-range";

interface Initial {
  filters?: Record<string, string>;
  sorting?: SortingState;
  range?: DateRange;
  size?: number;
}

/** The state behind a server table: search, filters, period, sorting and page. Any change but the page returns to the first page. */
export function useTableQuery(initial: Initial = {}) {
  const [defaults] = useState(() => ({ filters: initial.filters ?? {}, sorting: initial.sorting ?? [], range: initial.range ?? NO_RANGE }));
  const [search, setSearchState] = useState("");
  const [filters, setFilters] = useState<Record<string, string>>(defaults.filters);
  const [range, setRangeState] = useState<DateRange>(defaults.range);
  const [sorting, setSortingState] = useState<SortingState>(defaults.sorting);
  const [page, setPage] = useState(0);
  const [size, setSizeState] = useState(initial.size ?? 20);
  const q = useDebounced(search);

  const setSearch = useCallback((value: string) => { setSearchState(value); setPage(0); }, []);
  const setFilter = useCallback((key: string, value: string) => { setFilters((current) => ({ ...current, [key]: value })); setPage(0); }, []);
  const setRange = useCallback((value: DateRange) => { setRangeState(value); setPage(0); }, []);
  const setSorting = useCallback((value: SortingState) => { setSortingState(value); setPage(0); }, []);
  const setSize = useCallback((value: number) => { setSizeState(value); setPage(0); }, []);
  const reset = useCallback(() => {
    setSearchState(""); setFilters(defaults.filters); setRangeState(defaults.range); setPage(0);
  }, [defaults]);

  const sort = sorting[0] ? `${sorting[0].id},${sorting[0].desc ? "desc" : "asc"}` : undefined;
  /** True when the period differs from the page's own default, so only a deliberate choice shows as an applied filter. */
  const rangeChanged = range.preset !== defaults.range.preset || range.from !== defaults.range.from || range.to !== defaults.range.to;
  return { search, q, setSearch, rangeChanged, filters, setFilter, range, setRange, sorting, setSorting, sort, page, setPage, size, setSize, reset };
}

export type TableQuery = ReturnType<typeof useTableQuery>;
