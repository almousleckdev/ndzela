# ADR-0011: TanStack Table v9 for every table, and one console design system

**Status:** Accepted · **Date:** 2026-10-01

**Context.** Each list screen had its own hand-built table, search box, tabs and pager, so filters looked and behaved differently from page to page, and the sidebar and dashboard mixed unrelated items. Staff work in these tables all day.

**Decision.**
- **One table component** (`components/data-table/data-table.tsx`) built on **TanStack Table v9** (`useTable`, `tableFeatures`): sorting, pagination and column visibility, in two modes. **Server mode** (vehicles, drivers, payments, arrears, history, recharges, audit): the API does the search, filter, sort, page and period; the table only displays (`manualSorting`, `manualPagination`, `rowCount`). **Browser mode** (users, a short list): the table sorts and pages in memory. The row markup is ours (density, focus, a 3 px level marker on the row edge); the library owns state and models.
- **One toolbar vocabulary**: `SearchField`, `FilterPill` (single choice, shows its value), `DateRangePicker` (quick periods and a calendar), `ActiveFilters` (removable chips, "Tout effacer"), and the `useTableQuery` hook holding search, filters, period, sort and page (any change but the page returns to page 1).
- **API contract for lists**: `q` (search), `sort=field,asc|desc` against a whitelist (`SortParam`, unknown field answers 400 `INVALID_PARAMETER`), `from` and `to` (whole days, inclusive, in each country's timezone, `DayRanges`), plus the list's own filters.
- **Design system**: tokens in `globals.css` (ink navy, cool canvas, hairline, brand orange for actions and focus, signal colours only for state), IBM Plex Sans with tabular numerals plus Bricolage Grotesque for titles; flat bordered panels instead of shadowed cards; `KpiBand`, `PageHead`, `Panel`, `Toolbar` for every page; a grouped sidebar (Pilotage, Finance, Énergie, Flotte, Administration) with live counters and a mobile drawer; status as a dot and a word.

**Consequences.** (+) One look and behaviour, filters you can read and clear, sorting that is correct across pages, accessible markup. (-) TanStack Table v9 is a new major with a smaller body of examples; the dependency set grows (`@tanstack/react-table`, `react-day-picker`, `date-fns`). New tables must use the shared pieces rather than their own markup.
