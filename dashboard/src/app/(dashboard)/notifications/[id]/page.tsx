import { NotificationDetail } from "@/features/notifications/components/notification-detail";

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <NotificationDetail id={id} />;
}
