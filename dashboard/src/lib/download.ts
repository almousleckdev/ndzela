import { apiBlob } from "@/lib/api/api-client";

// Downloads a protected file (an export) and hands it to the browser as a file save.
export async function downloadFile(endpoint: string, filename: string): Promise<void> {
  const url = URL.createObjectURL(await apiBlob(endpoint));
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}
