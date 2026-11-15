import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { TodaySessionsPanel } from "@/features/dashboard/components/today-sessions-panel";
import type { ChargingSessionRow } from "@/features/charging/types";

const session = (patch: Partial<ChargingSessionRow>) => ({ id: "s1", startedAt: "2026-11-15T17:50:00Z", energyKwh: 12.5, amount: 4375, holder: null, ...patch }) as ChargingSessionRow;

describe("today's sessions on the overview", () => {
  it("shows the driver's name and the amount of a network session", () => {
    render(<TodaySessionsPanel sessions={[session({ holder: { driverId: "d1", driverName: "Awa Nze", vehicleId: null, vehicle: "LZ-105-AA" } })]} />);

    expect(screen.getByText("Awa Nze")).toBeInTheDocument();
    expect(screen.getByText(/12,5 kWh/)).toBeInTheDocument();
  });

  it("calls a session without a driver a public customer", () => {
    render(<TodaySessionsPanel sessions={[session({})]} />);

    expect(screen.getByText("Client public")).toBeInTheDocument();
  });

  it("says so when nothing was charged today", () => {
    render(<TodaySessionsPanel sessions={[]} />);

    expect(screen.getByText(/Aucune recharge aujourd/)).toBeInTheDocument();
  });
});
