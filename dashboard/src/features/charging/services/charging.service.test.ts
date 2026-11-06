import { beforeEach, describe, expect, it, vi } from "vitest";

const apiClient = vi.hoisted(() => vi.fn());
vi.mock("@/lib/api/api-client", () => ({ apiClient }));

import { chargingService } from "@/features/charging/services/charging.service";

describe("the charging sessions requests", () => {
  beforeEach(() => apiClient.mockReset().mockResolvedValue({}));

  it("asks for the sessions of one kind", async () => {
    await chargingService.sessions({ kind: "PUBLIC" }, 0, 20);

    expect(apiClient).toHaveBeenCalledWith(expect.stringContaining("kind=PUBLIC"));
  });

  it("leaves the kind out when every session is wanted", async () => {
    await chargingService.sessions({ kind: "ALL" }, 0, 20);

    expect(apiClient).toHaveBeenCalledWith(expect.not.stringContaining("kind="));
  });

  it("follows the kind for the totals and the export", async () => {
    await chargingService.sessionSummary("DRIVER");
    expect(apiClient).toHaveBeenCalledWith("/charging/sessions/summary?kind=DRIVER");

    expect(chargingService.exportSessionsPath({ q: "", from: "", to: "", kind: "PUBLIC" }, "XLSX")).toContain("kind=PUBLIC");
  });
});
