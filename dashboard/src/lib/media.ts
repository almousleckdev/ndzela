import { env } from "@/config/env";

// Pictures come from the CDN as full https URLs; an older relative path is still served by the backend.
export function mediaUrl(path: string | null): string | null {
  if (!path) return null;
  if (path.startsWith("https://")) return path;
  return `${new URL(env.NEXT_PUBLIC_API_URL).origin}${path}`;
}
