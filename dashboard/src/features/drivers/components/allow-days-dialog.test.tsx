import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { AllowDaysDialog } from "@/features/drivers/components/allow-days-dialog";

const account = vi.fn();
const allowDays = vi.fn();
vi.mock("@/features/payments/services/payments.service", () => ({ paymentsService: { account: (id: string) => account(id), allowDays: (id: string, days: string[]) => allowDays(id, days) } }));
vi.mock("@/hooks/use-toast", () => ({ useToast: () => ({ addToast: vi.fn() }) }));

const day = (iso: string, allowed: boolean, late = true) => ({ day: iso, dueAt: `${iso}T12:00:00+01:00`, rent: 23000, penalty: 0, amount: 23000, late, allowed });
const props = { driverId: "d1", driverName: "ABA Paul", onClose: vi.fn(), onSaved: vi.fn() };

describe("AllowDaysDialog", () => {
  it("lists the late days with their deadline and sends the ticked ones", async () => {
    account.mockResolvedValue({ payableDays: [day("2026-10-01", false), day("2026-10-02", true), day("2026-10-05", false, false)] });
    allowDays.mockResolvedValue({});

    render(<AllowDaysDialog {...props} />);

    expect(await screen.findAllByRole("checkbox")).toHaveLength(2);
    expect(screen.getAllByText(/Échu le/)).toHaveLength(2);
    fireEvent.click(screen.getAllByRole("checkbox")[0]);
    fireEvent.click(screen.getByRole("button", { name: "Enregistrer" }));

    await waitFor(() => expect(allowDays).toHaveBeenCalledWith("d1", ["2026-10-01", "2026-10-02"]));
  });

  it("takes every day back with Tout retirer", async () => {
    account.mockResolvedValue({ payableDays: [day("2026-10-01", true)] });
    allowDays.mockResolvedValue({});

    render(<AllowDaysDialog {...props} />);
    fireEvent.click(await screen.findByRole("button", { name: "Tout retirer" }));

    await waitFor(() => expect(allowDays).toHaveBeenCalledWith("d1", []));
  });

  it("allows the oldest N days when a number is picked", async () => {
    account.mockResolvedValue({ payableDays: [day("2026-10-01", false), day("2026-10-02", false), day("2026-10-03", false)] });
    allowDays.mockResolvedValue({});

    render(<AllowDaysDialog {...props} />);
    fireEvent.change(await screen.findByLabelText("Nombre de jours autorisés"), { target: { value: "2" } });
    fireEvent.click(screen.getByRole("button", { name: "Enregistrer" }));

    await waitFor(() => expect(allowDays).toHaveBeenCalledWith("d1", ["2026-10-01", "2026-10-02"]));
  });
});
