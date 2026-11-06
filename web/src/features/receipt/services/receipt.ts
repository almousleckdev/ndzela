import type { PublicReceipt } from "@/features/receipt/types";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
const ID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

// The payment behind a scanned receipt, or null when there is none (or it cannot be read now).
export async function fetchReceipt(id: string): Promise<PublicReceipt | null> {
  if (!ID.test(id)) return null;
  try {
    const response = await fetch(`${API_URL}/api/v1/public/receipts/${id}`, { cache: "no-store" });
    return response.ok ? ((await response.json()) as PublicReceipt) : null;
  } catch {
    return null;
  }
}
