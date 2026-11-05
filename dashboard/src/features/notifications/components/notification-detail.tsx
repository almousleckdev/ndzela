"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { BackLink } from "@/components/layout/back-link";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { EmptyState } from "@/components/ui/empty-state";
import { PageHeader } from "@/components/ui/page-header";
import { Skeleton } from "@/components/ui/skeleton";
import { useAsync } from "@/hooks/use-async";
import { getErrorMessage } from "@/lib/errors";
import { formatDateTime } from "@/lib/time";
import { notificationTarget } from "@/features/notifications/lib/notification-target";
import { notificationsService } from "@/features/notifications/services/notifications.service";

// One notification in full: what happened, when, and where to deal with it.
export function NotificationDetail({ id }: { id: string }) {
  const router = useRouter();
  const notification = useAsync(() => notificationsService.get(id), [id]);

  // Opening it counts as reading it.
  useEffect(() => {
    if (notification.data && !notification.data.read) notificationsService.markRead(id).catch(() => undefined);
  }, [id, notification.data]);

  if (notification.error) return <EmptyState title="Notification introuvable" description={getErrorMessage(notification.error, "Elle a peut-être été supprimée.")} />;
  if (!notification.data) return <Skeleton className="mx-auto h-48 max-w-2xl" />;

  const n = notification.data;
  const target = notificationTarget(n.type);
  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <BackLink href="/" label="Tableau de bord" />
      <PageHeader title={n.title} description={`${target.label} · ${formatDateTime(n.createdAt)}`} />
      <Card>
        <p className="whitespace-pre-line break-words text-sm text-slate-700">{n.message}</p>
        {target.href && (
          <div className="mt-6 border-t border-slate-100 pt-4">
            <Button onClick={() => router.push(target.href!)}>{target.action}</Button>
          </div>
        )}
      </Card>
    </div>
  );
}
