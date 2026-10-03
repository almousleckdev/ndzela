export interface NotificationItem {
  id: string;
  type: string;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
}

export interface NotificationList {
  unreadCount: number;
  items: NotificationItem[];
}
