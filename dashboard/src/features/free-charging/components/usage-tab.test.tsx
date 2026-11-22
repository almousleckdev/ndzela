import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { UsageTab } from "@/features/free-charging/components/usage-tab";

const usage = vi.hoisted(() => vi.fn());
vi.mock("@/features/free-charging/services/free-charging.service", () => ({ freeChargingService: { usage } }));

beforeEach(() => vi.clearAllMocks());

describe("UsageTab", () => {
  it("lists the free sessions", async () => {
    usage.mockResolvedValue({ items: [{ sessionId: "s1", driverId: "d1", driverName: "Paul Aba", startedAt: "2026-11-15T13:00:00Z", energyKwh: 12.5, stopReason: "Local" }], page: 0, size: 20, total: 1 });
    render(<UsageTab />);

    expect(await screen.findByText("Paul Aba")).toBeInTheDocument();
    expect(screen.getByText("12.5 kWh")).toBeInTheDocument();
    expect(usage).toHaveBeenCalledWith(0, 20);
  });

  it("labels the end of the free window", async () => {
    usage.mockResolvedValue({ items: [{ sessionId: "s2", driverId: "d1", driverName: "Awa Nze", startedAt: "2026-11-15T13:00:00Z", energyKwh: 3, stopReason: "FREE_WINDOW_ENDED" }], page: 0, size: 20, total: 1 });
    render(<UsageTab />);

    expect(await screen.findByText("Fin du créneau gratuit (22h)")).toBeInTheDocument();
  });

  it("shows the empty state", async () => {
    usage.mockResolvedValue({ items: [], page: 0, size: 20, total: 0 });
    render(<UsageTab />);

    expect(await screen.findByText("Aucune recharge gratuite")).toBeInTheDocument();
  });
});
