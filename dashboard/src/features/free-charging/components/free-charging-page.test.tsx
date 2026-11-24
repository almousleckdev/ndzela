import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { FreeChargingPage } from "@/features/free-charging/components/free-charging-page";

const service = vi.hoisted(() => ({ list: vi.fn(), window: vi.fn(), drivers: vi.fn(), usage: vi.fn() }));
const granted = vi.hoisted(() => ({ manage: true }));

vi.mock("@/features/free-charging/services/free-charging.service", () => ({ freeChargingService: service }));
vi.mock("@/hooks/use-toast", () => ({ useToast: () => ({ addToast: vi.fn() }) }));
vi.mock("@/features/access/components/require-permission", () => ({ RequirePermission: ({ children }: { children: React.ReactNode }) => <>{children}</> }));
vi.mock("@/features/access/hooks/use-permissions", () => ({ usePermissions: () => ({ can: (permission: string) => permission === "free_charging:read" || (granted.manage && permission === "free_charging:manage") }) }));

beforeEach(() => {
  vi.clearAllMocks();
  granted.manage = true;
  service.list.mockResolvedValue([]);
  service.drivers.mockResolvedValue({ items: [], page: 0, size: 25, total: 0 });
  service.usage.mockResolvedValue({ items: [], page: 0, size: 20, total: 0 });
  service.window.mockResolvedValue({ day: "SUNDAY", from: "00:00", until: "23:59", endOfDay: true });
});

describe("FreeChargingPage", () => {
  it("shows the window card and no add modal button", async () => {
    render(<FreeChargingPage />);

    expect(await screen.findByText("Recharge gratuite le dimanche, de 00h00 à 23h59 (heure du Gabon).")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Ajouter un chauffeur" })).not.toBeInTheDocument();
  });

  it("asks only for the active drivers", async () => {
    render(<FreeChargingPage />);
    await screen.findByText(/Recharge gratuite le dimanche/);

    await waitFor(() => expect(service.drivers).toHaveBeenCalled());
  });

  it("never writes the old 12h-22h window anywhere", async () => {
    render(<FreeChargingPage />);
    await screen.findByText(/Recharge gratuite le dimanche/);

    expect(document.body).not.toHaveTextContent("12h");
    expect(document.body).not.toHaveTextContent("22h");
  });
});
