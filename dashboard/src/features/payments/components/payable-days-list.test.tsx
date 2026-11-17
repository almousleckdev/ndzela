import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import { PayableDaysList } from "@/features/payments/components/payable-days-list";
import type { PayableDay } from "@/features/payments/types";

const DAYS: PayableDay[] = [
  { day: "2026-10-01", dueAt: "2026-10-01T12:00:00+01:00", rent: 23000, penalty: 5000, amount: 28000, late: true, allowed: false },
  { day: "2026-10-02", dueAt: "2026-10-02T12:00:00+01:00", rent: 23000, penalty: 0, amount: 23000, late: true, allowed: false },
  { day: "2026-10-03", dueAt: "2026-10-03T12:00:00+01:00", rent: 23000, penalty: 0, amount: 23000, late: true, allowed: false },
];

function Harness({ onPick }: { onPick?: (days: string[]) => void }) {
  const [selected, setSelected] = useState<string[]>([]);
  return <PayableDaysList days={DAYS} selected={selected} onChange={(next) => { setSelected(next); onPick?.(next); }} />;
}

describe("PayableDaysList", () => {
  it("lists every day with its amount and shows the penalty of a day", () => {
    render(<Harness />);

    expect(screen.getAllByRole("checkbox")).toHaveLength(3);
    expect(screen.getByText(/pénalité/i)).toBeInTheDocument();
    expect(screen.getByText(/28\s000/)).toBeInTheDocument();
  });

  it("totals the ticked days, skipping any older one", async () => {
    const picks: string[][] = [];
    render(<Harness onPick={(days) => picks.push(days)} />);

    await userEvent.click(screen.getAllByRole("checkbox")[2]);
    await userEvent.click(screen.getAllByRole("checkbox")[0]);

    expect(picks.at(-1)).toEqual(["2026-10-01", "2026-10-03"]);
    expect(screen.getByText(/Total/)).toHaveTextContent(/51\s000/);
  });

  it("ticks everything with Tout sélectionner", async () => {
    render(<Harness />);

    await userEvent.click(screen.getByRole("button", { name: "Tout sélectionner" }));

    expect(screen.getAllByRole("checkbox").every((box) => (box as HTMLInputElement).checked)).toBe(true);
    expect(screen.getByText(/Total/)).toHaveTextContent(/74\s000/);
  });
});
