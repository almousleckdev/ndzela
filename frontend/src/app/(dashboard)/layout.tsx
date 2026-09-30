"use client";

import { Header } from "@/components/layout/header";
import { Sidebar } from "@/components/layout/sidebar";
import { Skeleton } from "@/components/ui/skeleton";
import { useRequireSession } from "@/features/auth/hooks/use-session";
import { useCountrySelectionStore } from "@/features/tenancy/store/country-selection.store";

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const isReady = useRequireSession();
  const selectedCountry = useCountrySelectionStore((state) => state.selectedCode);

  if (!isReady) {
    return (
      <div className="flex h-screen w-full items-center justify-center bg-slate-50">
        <Skeleton className="h-12 w-12 rounded-full" />
      </div>
    );
  }

  return (
    <>
      <Sidebar />
      <div className="flex flex-1 flex-col overflow-hidden">
        <Header />
        {/* Keyed by the selected country so every page reloads its data when the super admin switches country. */}
        <main key={selectedCountry ?? "all"} className="flex-1 overflow-y-auto bg-slate-50 p-6">{children}</main>
      </div>
    </>
  );
}
