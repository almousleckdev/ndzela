import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { DriversTab } from "@/features/free-charging/components/drivers-tab";

const service = vi.hoisted(() => ({ list: vi.fn(), create: vi.fn(), update: vi.fn(), suspend: vi.fn(), resume: vi.fn(), revoke: vi.fn(), drivers: vi.fn() }));
const toast = vi.hoisted(() => vi.fn());
const granted = vi.hoisted(() => ({ manage: true }));

vi.mock("@/features/free-charging/services/free-charging.service", () => ({ freeChargingService: service }));
vi.mock("@/hooks/use-toast", () => ({ useToast: () => ({ addToast: toast }) }));
vi.mock("@/features/access/hooks/use-permissions", () => ({ usePermissions: () => ({ can: (permission: string) => permission === "free_charging:read" || (granted.manage && permission === "free_charging:manage") }) }));

const driver = (overrides: Record<string, unknown>) => ({
  driverId: "d-jean", customerId: "c-jean", fullName: "MBA Jean", phone: "+24177000001", email: null, fleetNumber: "V015", plate: "PL-0150", status: "ACTIVE",
  linked: true, inProgram: false, agreementId: null, agreementStatus: null, ...overrides,
});
const DRIVERS = [
  driver({}),
  driver({ driverId: "d-awa", fullName: "OBIANG Awa", inProgram: true, agreementId: "ag-awa", agreementStatus: "ACTIVE" }),
  driver({ driverId: "d-luc", fullName: "ONDO Luc", linked: false }),
];
const AGREEMENT = { id: "ag-awa", driverId: "d-awa", driverName: "OBIANG Awa", startsOn: "2026-11-01", endsOn: null, status: "ACTIVE", conditions: null, eligibility: "UNPAID_DAYS", unpaidDays: 3, lastFreeUse: null };

beforeEach(() => {
  vi.clearAllMocks();
  granted.manage = true;
  service.list.mockResolvedValue([AGREEMENT]);
  service.drivers.mockResolvedValue({ items: DRIVERS, page: 0, size: 20, total: DRIVERS.length });
  for (const name of ["create", "suspend", "resume", "revoke", "update"] as const) service[name].mockResolvedValue({});
});

describe("DriversTab", () => {
  it("lists the drivers with their programme state and eligibility", async () => {
    render(<DriversTab />);

    expect(await screen.findByText("MBA Jean")).toBeInTheDocument();
    expect(screen.getByText("Hors programme")).toBeInTheDocument();
    expect(screen.getByText("Compte Zella non lié", { selector: "span" })).toBeInTheDocument();
    expect(screen.getByText("Jours impayés", { selector: "td *, td" })).toBeInTheDocument();
  });

  it("adds a driver with one click, no dialog", async () => {
    render(<DriversTab />);
    await userEvent.click(await screen.findByRole("button", { name: "Ajouter MBA Jean" }));

    expect(service.create).toHaveBeenCalledWith("d-jean", { startsOn: expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/), endsOn: null, conditions: null });
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    await waitFor(() => expect(toast).toHaveBeenCalledWith(expect.objectContaining({ type: "success" })));
  });

  it("removes a driver with one click", async () => {
    render(<DriversTab />);
    await userEvent.click(await screen.findByRole("button", { name: "Retirer OBIANG Awa" }));

    expect(service.revoke).toHaveBeenCalledWith("ag-awa");
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("cannot add a driver whose Zella account is not linked", async () => {
    render(<DriversTab />);

    expect(await screen.findByRole("button", { name: "Ajouter ONDO Luc" })).toBeDisabled();
  });

  it("hides every management button without free_charging:manage", async () => {
    granted.manage = false;
    render(<DriversTab />);
    await screen.findByText("MBA Jean");

    for (const name of [/^Ajouter/, /^Modifier/, /^Suspendre/, /^Reprendre/, /^Retirer/]) expect(screen.queryByRole("button", { name })).not.toBeInTheDocument();
  });
});
