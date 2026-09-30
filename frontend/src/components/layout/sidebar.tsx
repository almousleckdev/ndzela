"use client";

import { useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { PanelLeftClose, PanelLeftOpen } from "lucide-react";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { cn } from "@/lib/utils";
import { NAV_ITEMS } from "@/components/layout/nav-items";

export function Sidebar({ className }: { className?: string }) {
  const pathname = usePathname();
  const { can } = usePermissions();
  const [collapsed, setCollapsed] = useState(false);
  const items = NAV_ITEMS.filter((item) => !item.permission || can(item.permission));

  return (
    <aside className={cn("flex h-full flex-col bg-sidebar text-slate-300 transition-[width] duration-200", collapsed ? "w-[72px]" : "w-64", className)}>
      <div className={cn("relative flex items-center justify-center border-b border-slate-800", collapsed ? "flex-col gap-1 py-3" : "h-20")}>
        <Image src="/images/logo.png" alt="NDZela" width={56} height={56} priority className={cn("w-auto", collapsed ? "h-9" : "h-14")} />
        <div className={cn(!collapsed && "absolute right-3 top-1/2 -translate-y-1/2")}>
          <CollapseButton collapsed={collapsed} onClick={() => setCollapsed((value) => !value)} />
        </div>
      </div>

      <nav aria-label="Navigation principale" className="flex-1 space-y-1 overflow-y-auto px-3 py-3">
        {items.map((item) => {
          const isActive = item.href === "/" ? pathname === "/" : pathname.startsWith(item.href);
          return (
            <Link
              key={item.href}
              title={collapsed ? item.label : undefined}
              href={item.href}
              aria-current={isActive ? "page" : undefined}
              className={cn(
                "group flex items-center rounded-md py-2.5  text-sm font-medium transition-colors hover:bg-slate-800 hover:text-white",
                collapsed ? "justify-center px-0" : "px-3",
                isActive && "bg-slate-800 text-white"
              )}
            >
              <item.icon className={cn("h-5 w-5 shrink-0 transition-colors group-hover:text-primary", !collapsed && "mr-3", isActive ? "text-primary" : "text-slate-400")} />
              {!collapsed && item.label}
            </Link>
          );
        })}
      </nav>

    </aside>
  );
}

function CollapseButton({ collapsed, onClick }: { collapsed: boolean; onClick: () => void }) {
  const Icon = collapsed ? PanelLeftOpen : PanelLeftClose;
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={collapsed ? "Développer le menu" : "Réduire le menu"}
      className="rounded-md p-2 text-slate-400 transition-colors hover:bg-slate-800 hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
    >
      <Icon className="h-5 w-5" />
    </button>
  );
}
