import { apiClient } from "@/lib/api/api-client";
import type { TrainingCenter } from "@/features/drivers/types";

export const trainingCentersService = {
  list: () => apiClient<TrainingCenter[]>("/training-centers"),
  create: (name: string) => apiClient<TrainingCenter>("/training-centers", { method: "POST", body: { name } }),
  update: (id: string, name: string) => apiClient<TrainingCenter>(`/training-centers/${id}`, { method: "PUT", body: { name } }),
  remove: (id: string) => apiClient(`/training-centers/${id}`, { method: "DELETE" }),
};
