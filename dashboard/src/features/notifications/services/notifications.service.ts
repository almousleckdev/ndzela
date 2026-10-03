import { apiClient } from "@/lib/api/api-client";
import type { NotificationList } from "@/features/notifications/types";

export const notificationsService = {
  list: () => apiClient<NotificationList>("/notifications"),
  markRead: (id: string) => apiClient(`/notifications/${id}/read`, { method: "POST" }),
  markAllRead: () => apiClient("/notifications/read-all", { method: "POST" }),
};
