import type { Page } from "@playwright/test";
import { MockApi } from "./mock-api";

export const GABON = { id: "country-ga", code: "GA", name: "Gabon", currencyCode: "XAF", phoneCode: "+241", timezone: "Africa/Libreville", defaultLanguage: "fr" };

export const profile = (permissions: string[]) => ({
  email: "manager@ndzela.test", firstName: "Awa", lastName: "Nze", phoneNumber: null, profilePictureUrl: null, mfaEnabled: true,
  roles: ["GESTIONNAIRE"], permissions, isGlobal: false, countries: [GABON], country: GABON,
});

// A signed-in session: the refresh cookie is accepted and the profile is loaded.
export function signedIn(api: MockApi, permissions: string[]): MockApi {
  return api
    .on("POST /auth/refresh", { accessToken: "token", tokenType: "Bearer", expiresInSeconds: 900 })
    .on("GET /profile/me", profile(permissions))
    .on("GET /countries", [GABON]);
}

export const page0 = (items: unknown[] = []) => ({ items, page: 0, size: 20, total: items.length, totalPages: items.length ? 1 : 0 });

export async function open(page: Page, api: MockApi, path: string): Promise<void> {
  await api.attach(page);
  await page.goto(path);
}
