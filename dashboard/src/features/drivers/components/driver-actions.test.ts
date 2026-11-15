import { describe, expect, it, vi } from "vitest";
import { PERMISSIONS, type Permission } from "@/features/access/lib/permissions";
import { availableActions } from "@/features/drivers/components/driver-actions";
import type { Driver } from "@/features/drivers/types";

// The actions only list what is offered: the services behind them are not needed (and would need the API address).
vi.mock("@/features/drivers/services/drivers.service", () => ({ driversService: {} }));
vi.mock("@/lib/api/api-client", () => ({ apiClient: vi.fn() }));

const driver = { status: "ACTIVE", loginLocked: false, vehicle: null } as unknown as Driver;
const holding = (...held: Permission[]) => (permission: Permission) => held.includes(permission);

describe("availableActions: helping a driver get back in", () => {
  it("offers the new email/phone and the temporary password with drivers:credentials", () => {
    const kinds = availableActions(driver, holding(PERMISSIONS.DRIVERS_CREDENTIALS)).map((action) => action.kind);

    expect(kinds).toContain("change-contact");
    expect(kinds).toContain("reset-password");
  });

  it("offers neither without the permission", () => {
    const kinds = availableActions(driver, holding(PERMISSIONS.DRIVERS_UPDATE, PERMISSIONS.DRIVERS_SUSPEND)).map((action) => action.kind);

    expect(kinds).not.toContain("change-contact");
    expect(kinds).not.toContain("reset-password");
  });

  it("labels them in French", () => {
    const labels = availableActions(driver, holding(PERMISSIONS.DRIVERS_CREDENTIALS)).map((action) => action.label);

    expect(labels).toEqual(["Modifier l'email ou le téléphone", "Nouveau mot de passe"]);
  });
});
