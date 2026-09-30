export default function Dashboard() {
  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
        {[1, 2, 3, 4].map((i) => (
          <div key={i} className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
            <h3 className="text-sm font-medium text-slate-500">Metric {i}</h3>
            <p className="mt-2 text-3xl font-semibold text-slate-900">12,345</p>
          </div>
        ))}
      </div>
      
      <div className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <h3 className="text-lg font-medium text-slate-900 mb-4">Recent Activity</h3>
        <div className="h-64 rounded-md border border-dashed border-slate-300 flex items-center justify-center bg-slate-50">
          <span className="text-slate-500 text-sm">Activity feed will appear here</span>
        </div>
      </div>
    </div>
  );
}
