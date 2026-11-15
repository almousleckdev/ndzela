import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { TemporaryPasswordDialog } from "@/features/drivers/components/temporary-password-dialog";

const open = (onConfirm = vi.fn().mockResolvedValue(undefined)) => {
  render(<TemporaryPasswordDialog driverName="Awa Nze" onClose={() => {}} onConfirm={onConfirm} />);
  return onConfirm;
};
const field = (label: string) => screen.getByLabelText(label) as HTMLInputElement;

describe("TemporaryPasswordDialog", () => {
  it("reminds the admin to tell the driver and that it must be changed", () => {
    open();

    expect(screen.getByText(/Communiquez ce mot de passe au chauffeur/)).toBeInTheDocument();
  });

  it("fills both fields with a generated password", async () => {
    open();

    await userEvent.click(screen.getByRole("button", { name: "Générer" }));

    expect(field("Mot de passe provisoire").value).toHaveLength(12);
    expect(field("Confirmer le mot de passe").value).toBe(field("Mot de passe provisoire").value);
  });

  it("copies the password", async () => {
    const user = userEvent.setup();
    open();

    await user.click(screen.getByRole("button", { name: "Générer" }));
    await user.click(screen.getByRole("button", { name: "Copier" }));

    expect(await navigator.clipboard.readText()).toBe(field("Mot de passe provisoire").value);
  });

  it("refuses two passwords that differ", async () => {
    const onConfirm = open();

    await userEvent.type(field("Mot de passe provisoire"), "Temp-Pass-2026");
    await userEvent.type(field("Confirmer le mot de passe"), "Temp-Pass-2027");
    await userEvent.click(screen.getByRole("button", { name: "Enregistrer" }));

    expect(await screen.findByText("Les mots de passe ne correspondent pas")).toBeInTheDocument();
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it("refuses a password shorter than 8 characters", async () => {
    const onConfirm = open();

    await userEvent.type(field("Mot de passe provisoire"), "short");
    await userEvent.type(field("Confirmer le mot de passe"), "short");
    await userEvent.click(screen.getByRole("button", { name: "Enregistrer" }));

    expect(await screen.findByText("Le mot de passe doit contenir au moins 8 caractères")).toBeInTheDocument();
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it("sends the password when both match", async () => {
    const onConfirm = open();

    await userEvent.type(field("Mot de passe provisoire"), "Temp-Pass-2026");
    await userEvent.type(field("Confirmer le mot de passe"), "Temp-Pass-2026");
    await userEvent.click(screen.getByRole("button", { name: "Enregistrer" }));

    await waitFor(() => expect(onConfirm).toHaveBeenCalledWith("Temp-Pass-2026"));
  });
});
