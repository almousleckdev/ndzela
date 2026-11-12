import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { PatternCasesPanel } from "@/features/payments/components/pattern-cases-panel";
import { paymentsService } from "@/features/payments/services/payments.service";

vi.mock("@/features/payments/services/payments.service", () => ({ paymentsService: { suspensionPatterns: vi.fn() } }));

describe("PatternCasesPanel", () => {
  beforeEach(() => vi.mocked(paymentsService.suspensionPatterns).mockReset());

  it("lists an account that looks like two cars with the drivers suspended meanwhile", async () => {
    vi.mocked(paymentsService.suspensionPatterns).mockResolvedValue([
      { account: { id: "d2", name: "Awa Mba", vehicle: "V-002 · GA-222" }, score: 90, reasons: ["OVERLAP", "ENERGY"], suspendedDrivers: ["Moussavou Paul"] },
    ]);

    render(<PatternCasesPanel />);

    await waitFor(() => expect(screen.getByText("Awa Mba")).toBeInTheDocument());
    expect(screen.getByText("Deux véhicules chargés en même temps · Énergie très supérieure à l'habitude")).toBeInTheDocument();
    expect(screen.getByText("90 / 100")).toBeInTheDocument();
    expect(screen.getByText("Moussavou Paul")).toBeInTheDocument();
  });

  it("says plainly when no account looks suspicious", async () => {
    vi.mocked(paymentsService.suspensionPatterns).mockResolvedValue([]);

    render(<PatternCasesPanel />);

    await waitFor(() => expect(screen.getByText("Aucun compte suspect")).toBeInTheDocument());
  });
});
