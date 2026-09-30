import { env } from "@/config/env";
import { useAuthStore } from "@/features/auth/store/auth.store";
import type { AuthResponse } from "@/features/auth/types";
import { ApiException, type ApiErrorResponse } from "@/lib/api/api-error";

interface FetchOptions extends Omit<RequestInit, "body"> {
  /** Serialised as JSON, unless it is FormData (file upload). */
  body?: unknown;
  /** Set to false for public endpoints (login, reset...). */
  requiresAuth?: boolean;
}

/** The single in-flight refresh. The refresh cookie is single-use, so concurrent callers must share one call. */
let refreshInFlight: Promise<boolean> | null = null;

/**
 * Exchanges the HttpOnly refresh cookie for a new access token.
 * @returns true when a new session was established.
 */
export function refreshSession(): Promise<boolean> {
  refreshInFlight ??= (async () => {
    try {
      const response = await fetch(`${env.NEXT_PUBLIC_API_URL}/auth/refresh`, {
        method: "POST",
        credentials: "include",
      });
      if (!response.ok) return false;
      const data = (await response.json()) as AuthResponse;
      useAuthStore.getState().setAccessToken(data.accessToken);
      return true;
    } catch {
      return false;
    } finally {
      refreshInFlight = null;
    }
  })();
  return refreshInFlight;
}

function send(endpoint: string, options: FetchOptions, requestId: string): Promise<Response> {
  const { body, requiresAuth = true, headers, ...init } = options;
  const finalHeaders = new Headers(headers);
  finalHeaders.set("X-Request-ID", requestId);
  const isForm = body instanceof FormData;
  if (body !== undefined && !isForm) finalHeaders.set("Content-Type", "application/json");

  const { accessToken } = useAuthStore.getState();
  if (requiresAuth && accessToken) finalHeaders.set("Authorization", `Bearer ${accessToken}`);

  return fetch(`${env.NEXT_PUBLIC_API_URL}${endpoint}`, {
    ...init,
    headers: finalHeaders,
    credentials: "include",
    body: body === undefined ? undefined : isForm ? body : JSON.stringify(body),
  });
}

async function toApiException(response: Response, endpoint: string, requestId: string): Promise<ApiException> {
  try {
    return new ApiException((await response.json()) as ApiErrorResponse);
  } catch {
    return new ApiException({
      timestamp: new Date().toISOString(),
      status: response.status,
      code: "UNKNOWN_ERROR",
      message: "An unexpected error occurred.",
      path: endpoint,
      requestId,
    });
  }
}

/**
 * Fetch wrapper: JSON in/out, cookie credentials, and a transparent one-time refresh
 * when the access token has expired.
 */
export async function apiClient<T>(endpoint: string, options: FetchOptions = {}): Promise<T> {
  const requestId = crypto.randomUUID();
  let response = await send(endpoint, options, requestId);

  if (response.status === 401 && (options.requiresAuth ?? true)) {
    if (await refreshSession()) {
      response = await send(endpoint, options, requestId);
    } else {
      useAuthStore.getState().clear();
      throw new ApiException({
        timestamp: new Date().toISOString(),
        status: 401,
        code: "SESSION_EXPIRED",
        message: "Session expired.",
        path: endpoint,
        requestId,
      });
    }
  }

  if (!response.ok) throw await toApiException(response, endpoint, requestId);

  const text = response.status === 204 ? "" : await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}
