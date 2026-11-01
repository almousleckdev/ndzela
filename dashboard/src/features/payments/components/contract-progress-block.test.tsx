import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ContractProgressBlock } from "@/features/payments/components/contract-progress-block";
import type { ContractProgress } from "@/features/payments/types";

const progress: ContractProgress = {
  totalDays: 1248, elapsedDays: 12, daysLeft: 1236, paidDays: 10, unpaidDays: 2,
  totalPrice: 28704000, paidAmount: 230000, remainingAmount: 28474000, deposit: 500000,
};

describe("ContractProgressBlock", () => {
  it("shows the day count, the days left and the deposit", () => {
    render(<ContractProgressBlock progress={progress} />);
    expect(screen.getByText(/Jour 12 sur 1.248/)).toBeInTheDocument();
    expect(screen.getByText(/1.236 restants/)).toBeInTheDocument();
    expect(screen.getByText(/caution 500 000 FCFA/)).toBeInTheDocument();
  });

  it("shows paid and unpaid days and the money paid and left", () => {
    render(<ContractProgressBlock progress={progress} />);
    expect(screen.getByText("Jours payés").nextSibling).toHaveTextContent(/10 \/ 12/);
    expect(screen.getByText("Jours impayés").nextSibling).toHaveTextContent("2");
    expect(screen.getByText("Total payé").nextSibling).toHaveTextContent("230 000 FCFA");
    expect(screen.getByText("Reste à régler").nextSibling).toHaveTextContent("28 474 000 FCFA");
  });

  it("draws the bar from the elapsed share, never past 100 %", () => {
    const { rerender } = render(<ContractProgressBlock progress={progress} />);
    expect(screen.getByRole("progressbar")).toHaveAttribute("aria-valuenow", "1");
    rerender(<ContractProgressBlock progress={{ ...progress, elapsedDays: 5000 }} />);
    expect(screen.getByRole("progressbar")).toHaveAttribute("aria-valuenow", "100");
  });

  it("does not divide by zero without a total", () => {
    render(<ContractProgressBlock progress={{ ...progress, totalDays: 0, elapsedDays: 0 }} />);
    expect(screen.getByRole("progressbar")).toHaveAttribute("aria-valuenow", "0");
  });
});
