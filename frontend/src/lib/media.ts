import { env } from "@/config/env";

/** Uploaded files are served by the backend at /media/...; turn that relative path into a full URL. */
export function mediaUrl(path: string | null): string | null {
  return path ? `${new URL(env.NEXT_PUBLIC_API_URL).origin}${path}` : null;
}
