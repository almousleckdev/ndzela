// A label / value list used by the vehicle detail cards.
export function DetailRows({ rows }: { rows: [string, React.ReactNode][] }) {
  return (
    <dl className="divide-y divide-slate-100">
      {rows.map(([label, value]) => (
        <div key={label} className="flex justify-between gap-4 py-2 text-sm">
          <dt className="text-slate-500">{label}</dt>
          <dd className="text-right font-medium text-slate-900">{value || "-"}</dd>
        </div>
      ))}
    </dl>
  );
}
