import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { DaysHistory } from "@/features/payments/components/days-history";
import type { DayView } from "@/features/payments/types";

const days = (count: number) => Array.from({ length: count }, (_, i) => ({ day: `2026-10-${String(30 - i).padStart(2, "0")}`, dueAt: "", amount: 23000, status: "UNPAID", settledAt: null })) as unknown as DayView[];

describe("DaysHistory", () => {
  it("shows the last week and opens the whole history below it", () => {
    render(<DaysHistory days={days(20)} />);

    expect(screen.getAllByRole("listitem")).toHaveLength(7);
    fireEvent.click(screen.getByRole("button", { name: /Voir plus \(13 jours\)/ }));
    expect(screen.getAllByRole("listitem")).toHaveLength(20);
    fireEvent.click(screen.getByRole("button", { name: "Voir moins" }));
    expect(screen.getAllByRole("listitem")).toHaveLength(7);
  });

  it("offers no toggle when a week or less exists", () => {
    render(<DaysHistory days={days(5)} />);

    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });

  it("says so when nothing is due yet", () => {
    render(<DaysHistory days={[]} />);

    expect(screen.getByText("Aucun loyer échu pour l'instant.")).toBeInTheDocument();
  });
});
