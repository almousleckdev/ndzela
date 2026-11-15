import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ChangeContactDialog } from "@/features/drivers/components/change-contact-dialog";

const open = (onConfirm = vi.fn().mockResolvedValue(undefined)) => {
  render(<ChangeContactDialog driverName="Awa Nze" currentEmail="awa@example.com" currentPhone="+24162821624" phoneCode="+241" onClose={() => {}} onConfirm={onConfirm} />);
  return onConfirm;
};

describe("ChangeContactDialog", () => {
  it("shows what the driver has today", () => {
    open();

    expect(screen.getByText(/awa@example.com/)).toBeInTheDocument();
    expect(screen.getByText(/\+24162821624/)).toBeInTheDocument();
  });

  it("refuses an empty form", async () => {
    const onConfirm = open();

    await userEvent.click(screen.getByRole("button", { name: "Enregistrer" }));

    expect(await screen.findByText("Indiquez un nouvel email ou un nouveau numéro")).toBeInTheDocument();
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it("refuses an email that is not valid", async () => {
    const onConfirm = open();

    await userEvent.type(screen.getByLabelText("Nouvel email"), "not-an-email");
    await userEvent.click(screen.getByRole("button", { name: "Enregistrer" }));

    expect(await screen.findByText("Adresse email invalide")).toBeInTheDocument();
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it("sends the new email and the phone in international form", async () => {
    const onConfirm = open();

    await userEvent.type(screen.getByLabelText("Nouvel email"), "new@example.com");
    await userEvent.type(screen.getByLabelText("Nouveau numéro de téléphone"), "74 11 11 11");
    await userEvent.click(screen.getByRole("button", { name: "Enregistrer" }));

    await waitFor(() => expect(onConfirm).toHaveBeenCalledWith({ email: "new@example.com", phone: "+241 74111111" }));
  });

  it("leaves out what was not changed", async () => {
    const onConfirm = open();

    await userEvent.type(screen.getByLabelText("Nouvel email"), "only@example.com");
    await userEvent.click(screen.getByRole("button", { name: "Enregistrer" }));

    await waitFor(() => expect(onConfirm).toHaveBeenCalledWith({ email: "only@example.com" }));
  });
});
