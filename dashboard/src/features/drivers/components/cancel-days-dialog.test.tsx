import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { CancelDaysDialog } from "@/features/drivers/components/cancel-days-dialog";

const cancelDays = vi.fn();
const restoreDay = vi.fn();
vi.mock("@/features/payments/services/payments.service", () => ({
  paymentsService: {
    account: () => Promise.resolve({ payableDays: [{ day: "2026-10-01", amount: 23000, late: true }, { day: "2026-10-02", amount: 23000, late: true }] }),
    cancelledDays: () => Promise.resolve([{ id: "c1", day: "2026-09-30", reason: "Suspendu", cancelledBy: "admin", cancelledAt: "" }]),
    cancelDays: (id: string, days: string[], reason: string) => cancelDays(id, days, reason),
    restoreDay: (id: string, c: string) => restoreDay(id, c),
  },
}));
vi.mock("@/hooks/use-toast", () => ({ useToast: () => ({ addToast: vi.fn() }) }));

const props = { driverId: "d1", driverName: "ABA Paul", onClose: vi.fn(), onSaved: vi.fn() };

describe("CancelDaysDialog", () => {
  it("needs a day and a reason, then sends them", async () => {
    cancelDays.mockResolvedValue([]);
    render(<CancelDaysDialog {...props} />);
    const boxes = await screen.findAllByRole("checkbox");

    fireEvent.click(screen.getByRole("button", { name: "Annuler les jours" }));
    expect(await screen.findByText("Choisissez au moins un jour.")).toBeTruthy();

    fireEvent.click(boxes[1]);
    fireEvent.click(screen.getByRole("button", { name: "Annuler les jours" }));
    expect(await screen.findByText("Le motif est obligatoire.")).toBeTruthy();

    fireEvent.change(screen.getByLabelText("Motif *"), { target: { value: "Suspendu" } });
    fireEvent.click(screen.getByRole("button", { name: "Annuler les jours" }));
    await waitFor(() => expect(cancelDays).toHaveBeenCalledWith("d1", ["2026-10-02"], "Suspendu"));
  });

  it("restores an already cancelled day", async () => {
    restoreDay.mockResolvedValue([]);
    render(<CancelDaysDialog {...props} />);

    fireEvent.click(await screen.findByRole("button", { name: "Rétablir" }));

    await waitFor(() => expect(restoreDay).toHaveBeenCalledWith("d1", "c1"));
  });
});
