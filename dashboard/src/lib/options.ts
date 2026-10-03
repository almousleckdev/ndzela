// Turns a label map into select/filter options, in the given key order (the map's own order by default).
export function toOptions<K extends string>(labels: Record<K, string>, order?: readonly K[]): { value: K; label: string }[] {
  return (order ?? (Object.keys(labels) as K[])).map((value) => ({ value, label: labels[value] }));
}
