import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { EarningsDriversTable } from "@/features/payments/components/earnings-drivers-table";
import { EarningsMonthsTable } from "@/features/payments/components/earnings-months-table";
import type { EarningsDriver, EarningsMonth } from "@/features/payments/types";

const driver = (patch: Partial<EarningsDriver>): EarningsDriver => ({ driverId: "d1", name: "NZE Awa", status: "ACTIVE", former: false, contractNumber: "NDZ-1", totalPaid: 46000, lastPayment: null, ...patch });

describe("EarningsMonthsTable", () => {
  it("lists one line per month with its name in French", () => {
    const months: EarningsMonth[] = [{ month: "2026-10", collected: 23000, fees: 500, net: 22500 }, { month: "2026-11", collected: 23000, fees: 0, net: 23000 }];

    render(<EarningsMonthsTable months={months} />);

    expect(screen.getByText("octobre 2026")).toBeInTheDocument();
    expect(screen.getByText("novembre 2026")).toBeInTheDocument();
  });

  it("says so when nothing was collected", () => {
    render(<EarningsMonthsTable months={[]} />);

    expect(screen.getByText("Aucun paiement enregistré.")).toBeInTheDocument();
  });
});

describe("EarningsDriversTable", () => {
  it("marks a former driver", () => {
    render(<EarningsDriversTable rows={[driver({ name: "MBA Paul", former: true, status: "INACTIVE" })]} />);

    expect(screen.getByText("MBA Paul")).toBeInTheDocument();
    expect(screen.getByText("Ancien")).toBeInTheDocument();
  });

  it("does not mark a current driver", () => {
    render(<EarningsDriversTable rows={[driver({})]} />);

    expect(screen.queryByText("Ancien")).not.toBeInTheDocument();
  });

  it("says so when no driver paid", () => {
    render(<EarningsDriversTable rows={[]} />);

    expect(screen.getByText("Aucun paiement enregistré.")).toBeInTheDocument();
  });
});
