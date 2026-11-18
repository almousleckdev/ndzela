import { beforeEach, describe, expect, it, vi } from "vitest";

const apiClient = vi.hoisted(() => vi.fn());
vi.mock("@/lib/api/api-client", () => ({ apiClient }));

import { usersService } from "@/features/users/services/users.service";

describe("the staff access requests", () => {
  beforeEach(() => apiClient.mockReset().mockResolvedValue({}));

  it("reads the car and the free charging of a staff member", async () => {
    await usersService.staffAccess("u1");

    expect(apiClient).toHaveBeenCalledWith("/users/u1/staff-access");
  });

  it("saves a fleet car with free charging", async () => {
    await usersService.saveStaffAccess("u1", { vehicleId: "v1", freeCharging: true });

    expect(apiClient).toHaveBeenCalledWith("/users/u1/staff-access", { method: "PUT", body: { vehicleId: "v1", freeCharging: true } });
  });

  it("removes the access", async () => {
    await usersService.removeStaffAccess("u1");

    expect(apiClient).toHaveBeenCalledWith("/users/u1/staff-access", { method: "DELETE" });
  });
});
