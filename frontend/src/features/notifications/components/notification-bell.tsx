"use client";

import { useEffect } from "react";
import { Bell, CheckCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dropdown } from "@/components/ui/dropdown";
import { useFetch } from "@/hooks/use-fetch";
import { cn } from "@/lib/utils";
import { timeAgo } from "@/lib/time";
import { notificationsService } from "@/features/notifications/services/notifications.service";
import type { NotificationItem } from "@/features/notifications/types";

const POLL_INTERVAL_MS = 30_000;

export function NotificationBell() {
  const { data, reload } = useFetch(notificationsService.list);

  useEffect(() => {
    const timer = setInterval(reload, POLL_INTERVAL_MS);
    return () => clearInterval(timer);
  }, [reload]);

  const unread = data?.unreadCount ?? 0;
  const items = data?.items ?? [];

  const open = async (item: NotificationItem) => {
    if (!item.read) {
      await notificationsService.markRead(item.id).catch(() => undefined);
      reload();
    }
  };

  const markAll = async () => {
    await notificationsService.markAllRead().catch(() => undefined);
    reload();
  };

  return (
    <Dropdown
      className="w-96"
      trigger={
        <Button variant="ghost" size="icon" aria-label={unread ? `Notifications, ${unread} non lue(s)` : "Notifications"} className="relative rounded-full text-slate-500">
          <Bell className="h-5 w-5" />
          {unread > 0 && (
            <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-primary px-1 text-[10px] font-bold text-white">
              {unread > 9 ? "9+" : unread}
            </span>
          )}
        </Button>
      }
    >
      <div className="flex items-center justify-between border-b border-slate-100 px-4 py-2">
        <span className="text-sm font-semibold text-slate-800">Notifications</span>
        {unread > 0 && (
          <Button variant="link" size="sm" onClick={markAll} className="h-auto p-0">
            <CheckCheck className="mr-1 h-3.5 w-3.5" /> Tout marquer comme lu
          </Button>
        )}
      </div>

      {items.length === 0 ? (
        <p className="px-4 py-8 text-center text-sm text-slate-500">Aucune notification.</p>
      ) : (
        <ul className="max-h-96 overflow-y-auto">
          {items.map((item) => (
            <li key={item.id}>
              <button
                type="button"
                onClick={() => open(item)}
                className={cn("flex w-full gap-3 px-4 py-3 text-left transition-colors hover:bg-slate-50", !item.read && "bg-orange-50/60")}
              >
                <span className={cn("mt-1.5 h-2 w-2 shrink-0 rounded-full", item.read ? "bg-transparent" : "bg-primary")} />
                <span className="min-w-0">
                  <span className="block text-sm font-medium text-slate-900">{item.title}</span>
                  <span className="block text-sm text-slate-600">{item.message}</span>
                  <span className="mt-0.5 block text-xs text-slate-400">{timeAgo(item.createdAt)}</span>
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </Dropdown>
  );
}
