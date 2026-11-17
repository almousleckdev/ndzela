import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { CloseContractDialog } from "@/features/drivers/components/close-contract-dialog";

const account = vi.fn();
vi.mock("@/features/payments/services/payments.service", () => ({ paymentsService: { account: (id: string) => account(id) } }));
vi.mock("@/features/payments/components/record-payment-dialog", () => ({ RecordPaymentDialog: () => <p>encaisser-dialog</p> }));
vi.mock("@/hooks/use-toast", () => ({ useToast: () => ({ addToast: vi.fn() }) }));

const props = { driverId: "d1", driverName: "ABA Paul", onClose: vi.fn(), onConfirm: vi.fn() };

describe("CloseContractDialog", () => {
  it("blocks the closing while late rent is owed and offers Encaisser to a cashier", async () => {
    account.mockResolvedValue({ unpaid: 2, overdueAmount: 46000, dailyRent: 23000 });

    render(<CloseContractDialog {...props} mayCollect />);

    expect(await screen.findByText("Des loyers restent à payer")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Encaisser" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Clôturer" })).not.toBeInTheDocument();
  });

  it("does not offer Encaisser without payments:create", async () => {
    account.mockResolvedValue({ unpaid: 1, overdueAmount: 23000, dailyRent: 23000 });

    render(<CloseContractDialog {...props} mayCollect={false} />);

    await screen.findByText("Des loyers restent à payer");
    expect(screen.queryByRole("button", { name: "Encaisser" })).not.toBeInTheDocument();
  });

  it("asks for the reason once nothing is owed", async () => {
    account.mockResolvedValue({ unpaid: 0, overdueAmount: 0, dailyRent: 23000 });

    render(<CloseContractDialog {...props} mayCollect />);

    expect(await screen.findByRole("button", { name: "Clôturer" })).toBeInTheDocument();
  });
});
