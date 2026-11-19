import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { RescheduleDaysDialog } from "@/features/drivers/components/reschedule-days-dialog";

const rescheduleDays = vi.fn();
const undoReschedule = vi.fn();
vi.mock("@/features/payments/services/payments.service", () => ({
  paymentsService: {
    account: () => Promise.resolve({ payableDays: [
      { day: "2026-10-01", amount: 23000, late: true }, { day: "2026-10-02", amount: 23000, late: true },
      { day: "2026-10-03", amount: 23000, late: true }, { day: "2026-10-05", amount: 23000, late: true }] }),
    rescheduledDays: () => Promise.resolve([{ id: "r1", originalDay: "2026-09-30", newDay: "2030-01-02", reason: "Panne", rescheduledBy: "admin", rescheduledAt: "" }]),
    rescheduleDays: (id: string, days: string[], reason: string) => rescheduleDays(id, days, reason),
    undoReschedule: (id: string, r: string) => undoReschedule(id, r),
  },
}));
vi.mock("@/hooks/use-toast", () => ({ useToast: () => ({ addToast: vi.fn() }) }));

const props = { driverId: "d1", driverName: "ABA Paul", onClose: vi.fn(), onSaved: vi.fn() };

describe("RescheduleDaysDialog", () => {
  it("sends the several days ticked, with the reason", async () => {
    rescheduleDays.mockResolvedValue([]);
    render(<RescheduleDaysDialog {...props} />);
    const boxes = await screen.findAllByRole("checkbox");

    fireEvent.click(boxes[0]);
    fireEvent.click(boxes[2]);
    fireEvent.click(boxes[3]);
    expect(screen.getByText("3 jours sélectionnés")).toBeTruthy();
    fireEvent.change(screen.getByLabelText("Motif *"), { target: { value: "Panne" } });
    fireEvent.click(screen.getByRole("button", { name: "Rééchelonner" }));

    await waitFor(() => expect(rescheduleDays).toHaveBeenCalledWith("d1", ["2026-10-01", "2026-10-03", "2026-10-05"], "Panne"));
  });

  it("selects every day at once", async () => {
    rescheduleDays.mockResolvedValue([]);
    render(<RescheduleDaysDialog {...props} />);
    await screen.findAllByRole("checkbox");

    fireEvent.click(screen.getByRole("button", { name: "Tout sélectionner" }));

    expect(screen.getByText("4 jours sélectionnés")).toBeTruthy();
  });

  it("puts a pushed day back", async () => {
    undoReschedule.mockResolvedValue([]);
    render(<RescheduleDaysDialog {...props} />);

    fireEvent.click(await screen.findByRole("button", { name: "Annuler le report" }));

    await waitFor(() => expect(undoReschedule).toHaveBeenCalledWith("d1", "r1"));
  });
});
