import { apiClient } from "@/lib/api/api-client";
import type { NotificationItem, NotificationList } from "@/features/notifications/types";

export const notificationsService = {
  list: () => apiClient<NotificationList>("/notifications"),
  get: (id: string) => apiClient<NotificationItem>(`/notifications/${id}`),
  markRead: (id: string) => apiClient(`/notifications/${id}/read`, { method: "POST" }),
  markAllRead: () => apiClient("/notifications/read-all", { method: "POST" }),
};
