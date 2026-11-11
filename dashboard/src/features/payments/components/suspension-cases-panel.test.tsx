import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SuspensionCasesPanel } from "@/features/payments/components/suspension-cases-panel";
import { paymentsService } from "@/features/payments/services/payments.service";
import type { SuspensionCase } from "@/features/payments/types";

vi.mock("@/features/payments/services/payments.service", () => ({ paymentsService: { suspensionCases: vi.fn() } }));

const suspendedDriver = { id: "d1", name: "Paul Moussavou", vehicle: "V-001 · GA-111" };

function aCase(overrides: Partial<SuspensionCase>): SuspensionCase {
  return { id: "c1", occurredAt: "2026-11-11T09:00:00Z", kind: "TOPUP_FROM_SUSPENDED_NUMBER", amountMinor: 5000, currency: "XAF", suspended: suspendedDriver, helper: null, helperReference: "abcdef0123456789", score: null, reasons: [], flagged: false, ...overrides };
}

describe("SuspensionCasesPanel", () => {
  beforeEach(() => vi.mocked(paymentsService.suspensionCases).mockReset());

  it("shows the suspended driver in full and an unknown helper only by a short reference", async () => {
    vi.mocked(paymentsService.suspensionCases).mockResolvedValue({ items: [aCase({})], page: 0, size: 20, total: 1 });

    render(<SuspensionCasesPanel />);

    await waitFor(() => expect(screen.getByText("Paul Moussavou")).toBeInTheDocument());
    expect(screen.getByText("Compte Zella")).toBeInTheDocument();
    expect(screen.getByText("réf. abcdef01")).toBeInTheDocument();
    expect(screen.queryByText("abcdef0123456789")).not.toBeInTheDocument();
  });

  it("names a helper who is one of our drivers and lists why the case is flagged", async () => {
    const flagged = aCase({ helper: { id: "d2", name: "Awa Mba", vehicle: "V-002 · GA-222" }, score: 100, flagged: true, reasons: ["OVERLAP"] });
    vi.mocked(paymentsService.suspensionCases).mockResolvedValue({ items: [flagged], page: 0, size: 20, total: 1 });

    render(<SuspensionCasesPanel />);

    await waitFor(() => expect(screen.getByText("Awa Mba")).toBeInTheDocument());
    expect(screen.getByText("Deux véhicules chargés en même temps")).toBeInTheDocument();
  });

  it("says plainly when nobody helped a suspended driver", async () => {
    vi.mocked(paymentsService.suspensionCases).mockResolvedValue({ items: [], page: 0, size: 20, total: 0 });

    render(<SuspensionCasesPanel />);

    await waitFor(() => expect(screen.getByText("Aucune aide détectée")).toBeInTheDocument());
  });
});
