"use client";

import { useEffect } from "react";
import Image from "next/image";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { PanelLeftClose, PanelLeftOpen, X } from "lucide-react";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { useShellStore } from "@/features/layout/shell-store";
import { useNavBadges } from "@/features/layout/use-nav-badges";
import { cn } from "@/lib/utils";
import { NAV_SECTIONS, type NavItem } from "@/components/layout/nav-items";

/** Desktop: a fixed rail that collapses to icons. Below `lg`: a drawer opened from the header. */
export function Sidebar() {
  const pathname = usePathname();
  const { collapsed, mobileOpen, toggleCollapsed, setMobileOpen } = useShellStore();

  useEffect(() => { setMobileOpen(false); }, [pathname, setMobileOpen]);
  useEffect(() => {
    if (!mobileOpen) return;
    const onKey = (event: KeyboardEvent) => { if (event.key === "Escape") setMobileOpen(false); };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [mobileOpen, setMobileOpen]);

  return (
    <>
      {mobileOpen && <div className="fixed inset-0 z-40 bg-slate-900/50 lg:hidden" onClick={() => setMobileOpen(false)} aria-hidden />}
      <aside aria-label="Navigation principale"
        className={cn("fixed inset-y-0 left-0 z-50 flex w-72 -translate-x-full flex-col bg-sidebar text-slate-300 transition-transform duration-200 lg:static lg:z-auto lg:translate-x-0 lg:transition-[width]",
          mobileOpen && "translate-x-0", collapsed ? "lg:w-[68px]" : "lg:w-64")}>
        <SidebarBody collapsed={collapsed && !mobileOpen} pathname={pathname} onToggle={toggleCollapsed} onClose={() => setMobileOpen(false)} />
      </aside>
    </>
  );
}

function SidebarBody({ collapsed, pathname, onToggle, onClose }: { collapsed: boolean; pathname: string; onToggle: () => void; onClose: () => void }) {
  const { can } = usePermissions();
  const badges = useNavBadges();
  const sections = NAV_SECTIONS.map((section) => ({ ...section, items: section.items.filter((item) => !item.permission || can(item.permission)) })).filter((section) => section.items.length > 0);

  return (
    <>
      <div className="relative flex h-16 shrink-0 items-center justify-center border-b border-white/10">
        <Image src="/images/logo.png" alt="NDZela" width={96} height={96} priority className={cn("w-auto", collapsed ? "h-10" : "h-12")} />
        <button type="button" onClick={onClose} aria-label="Fermer le menu" className="absolute right-3 top-1/2 -translate-y-1/2 rounded-md p-2 text-slate-400 hover:bg-white/10 hover:text-white lg:hidden"><X className="h-5 w-5" /></button>
      </div>

      <nav className="flex-1 overflow-y-auto px-3 py-2">
        {sections.map((section, index) => (
          <div key={section.label} className={cn(index > 0 && "mt-1")}>
            {collapsed ? (index > 0 && <div className="mx-2 my-2 border-t border-white/10" />) : <p className="px-3 pb-1 pt-4 text-xs font-medium text-slate-500">{section.label}</p>}
            <ul className="space-y-0.5">
              {section.items.map((item) => (
                <li key={item.href}><NavLink item={item} collapsed={collapsed} active={item.href === "/" ? pathname === "/" : pathname.startsWith(item.href)} count={item.badge === "arrears" ? badges.arrears : 0} /></li>
              ))}
            </ul>
          </div>
        ))}
      </nav>

      <div className="hidden shrink-0 border-t border-white/10 p-3 lg:block">
        <button type="button" onClick={onToggle} aria-label={collapsed ? "Développer le menu" : "Réduire le menu"}
          className={cn("flex h-9 w-full items-center gap-3 rounded-md px-3 text-sm text-slate-400 transition-colors hover:bg-white/5 hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary", collapsed && "justify-center px-0")}>
          {collapsed ? <PanelLeftOpen className="h-[18px] w-[18px]" /> : <><PanelLeftClose className="h-[18px] w-[18px]" /> Réduire</>}
        </button>
      </div>
    </>
  );
}

function NavLink({ item, active, collapsed, count }: { item: NavItem; active: boolean; collapsed: boolean; count: number }) {
  return (
    <Link href={item.href} title={collapsed ? item.label : undefined} aria-current={active ? "page" : undefined}
      className={cn("group relative flex h-10 items-center rounded-md text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary",
        collapsed ? "justify-center" : "gap-3 px-3", active ? "bg-white/10 text-white" : "text-slate-300 hover:bg-white/5 hover:text-white")}>
      {active && <span aria-hidden className="absolute inset-y-2 left-0 w-[3px] rounded-r bg-primary" />}
      <item.icon className={cn("h-[18px] w-[18px] shrink-0", active ? "text-primary" : "text-slate-400 group-hover:text-slate-200")} />
      {!collapsed && <span className="min-w-0 flex-1 truncate">{item.label}</span>}
      {count > 0 && (collapsed
        ? <span aria-label={`${count} en retard`} className="absolute right-2 top-2 h-2 w-2 rounded-full bg-red-500" />
        : <span aria-label={`${count} en retard`} className="rounded-full bg-red-500/90 px-1.5 py-0.5 text-[11px] font-semibold leading-none tabular-nums text-white">{count}</span>)}
    </Link>
  );
}
