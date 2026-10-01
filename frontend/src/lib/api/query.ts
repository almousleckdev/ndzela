type Value = string | number | boolean | null | undefined;

/** A query string from a plain object; empty, null and undefined values are left out. */
export function queryString(params: Record<string, Value>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null || value === "") continue;
    search.set(key, String(value));
  }
  return search.toString();
}
