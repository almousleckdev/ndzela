import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { PaidDays } from "@/features/payments/components/paid-days";

describe("PaidDays", () => {
  it("shows each day with its own late or on-time badge and its amount", () => {
    render(<PaidDays paidAt="2026-11-06T16:15:00+01:00" days={[
      { day: "2026-11-05", kind: "RENT", amount: 100, onTime: false },
      { day: "2026-11-06", kind: "RENT", amount: 100, onTime: true },
    ]} />);

    expect(screen.getAllByText("en retard")).toHaveLength(1);
    expect(screen.getAllByText("à temps")).toHaveLength(1);
    expect(screen.getAllByText(/100/)).toHaveLength(2);
  });
});
