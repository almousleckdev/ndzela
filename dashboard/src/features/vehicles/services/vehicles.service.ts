import { apiBlob, apiClient } from "@/lib/api/api-client";
import type { ExportFormat } from "@/components/export-menu";
import type { Page } from "@/lib/api/page";
import { queryString } from "@/lib/api/query";
import type { Vehicle, VehicleCounts, VehicleImage, VehicleQuery, VehicleStatus } from "@/features/vehicles/types";
import type { Driver } from "@/features/drivers/types";
import type { VehicleValues } from "@/features/vehicles/schemas/vehicle";

function toBody(values: VehicleValues) {
  return {
    fleetNumber: values.fleetNumber,
    brand: values.brand,
    model: values.model,
    year: Number(values.year),
    plate: values.plate,
    color: values.color,
    vin: values.vin.trim() || null,
    purchasePrice: Number(values.purchasePrice),
    dailyRent: Number(values.dailyRent),
  };
}

export const vehiclesService = {
  list: ({ status, q, from, to, sort, page, size }: VehicleQuery) => apiClient<Page<Vehicle>>(`/vehicles?${queryString({ status: status === "ALL" ? undefined : status, q: q.trim(), from, to, sort, page, size })}`),

  get: (id: string) => apiClient<Vehicle>(`/vehicles/${id}`),

  exportPath: ({ status, q, from, to }: Pick<VehicleQuery, "status" | "q" | "from" | "to">, format: ExportFormat) => `/vehicles/export?${queryString({ status: status === "ALL" ? undefined : status, q: q.trim(), from, to, format })}`,

  currentDriver: (id: string) => apiClient<Driver | undefined>(`/vehicles/${id}/driver`),

  counts: () => apiClient<VehicleCounts>("/vehicles/counts"),

  create: (values: VehicleValues) => apiClient<Vehicle>("/vehicles", { method: "POST", body: toBody(values) }),

  update: (id: string, values: VehicleValues) => apiClient<Vehicle>(`/vehicles/${id}`, { method: "PUT", body: toBody(values) }),

  changeStatus: (id: string, status: VehicleStatus, reason?: string) => apiClient<Vehicle>(`/vehicles/${id}/status`, { method: "POST", body: { status, reason } }),

  remove: (id: string) => apiClient(`/vehicles/${id}`, { method: "DELETE" }),

  uploadImages: (id: string, files: File[]) => {
    const body = new FormData();
    files.forEach((file) => body.append("files", file));
    return apiClient<VehicleImage[]>(`/vehicles/${id}/images`, { method: "POST", body });
  },

  // The photos with a displayable object URL each (the files are private, so they are fetched with the session).
  imagesWithUrls: async (id: string): Promise<(VehicleImage & { url: string })[]> => {
    const list = await apiClient<VehicleImage[]>(`/vehicles/${id}/images`);
    return Promise.all(list.map(async (image) => ({ ...image, url: URL.createObjectURL(await apiBlob(`/vehicles/${id}/images/${image.id}`)) })));
  },

  deleteImage: (id: string, imageId: string) => apiClient(`/vehicles/${id}/images/${imageId}`, { method: "DELETE" }),
};
