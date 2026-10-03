import { apiBlob, apiClient } from "@/lib/api/api-client";
import type { Page } from "@/lib/api/page";
import { queryString } from "@/lib/api/query";
import type { Application, ApplicationDecision, ApplicationFileKind, ApplicationStatus, ContactMessage, MessageStatus } from "@/features/inquiries/types";

export const inquiriesService = {
  applications: (status: ApplicationStatus | "", q: string, page: number, size: number) =>
    apiClient<Page<Application>>(`/applications?${queryString({ status: status || undefined, q: q.trim() || undefined, page, size })}`),
  updateApplication: (id: string, decision: ApplicationDecision) => apiClient<Application>(`/applications/${id}/status`, { method: "POST", body: decision }),
  // The file as a blob
  applicationFile: (id: string, kind: ApplicationFileKind) => apiBlob(`/applications/${id}/files/${kind}`),
  messages: (status: MessageStatus | "", q: string, page: number, size: number) =>
    apiClient<Page<ContactMessage>>(`/contact-messages?${queryString({ status: status || undefined, q: q.trim() || undefined, page, size })}`),
  handleMessage: (id: string) => apiClient<ContactMessage>(`/contact-messages/${id}/handle`, { method: "POST" }),
  newCount: async () => (await apiClient<{ count: number }>("/inquiries/new-count")).count,
};
