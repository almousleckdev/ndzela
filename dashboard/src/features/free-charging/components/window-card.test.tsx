import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiException } from "@/lib/api/api-error";
import { WindowCard } from "@/features/free-charging/components/window-card";
import type { FreeWindow } from "@/features/free-charging/types";

const service = vi.hoisted(() => ({ changeWindow: vi.fn() }));
const toast = vi.hoisted(() => vi.fn());
const granted = vi.hoisted(() => ({ manage: true }));

vi.mock("@/features/free-charging/services/free-charging.service", () => ({ freeChargingService: service }));
vi.mock("@/hooks/use-toast", () => ({ useToast: () => ({ addToast: toast }) }));
vi.mock("@/features/access/hooks/use-permissions", () => ({ usePermissions: () => ({ can: (permission: string) => permission === "free_charging:read" || (granted.manage && permission === "free_charging:manage") }) }));

const wholeSunday: FreeWindow = { day: "SUNDAY", from: "00:00", until: "23:59", endOfDay: true };
const onChanged = vi.fn();

beforeEach(() => {
  vi.clearAllMocks();
  granted.manage = true;
  service.changeWindow.mockResolvedValue(wholeSunday);
});

describe("WindowCard", () => {
  it("tells the current window in words", () => {
    render(<WindowCard window={wholeSunday} onChanged={onChanged} />);

    expect(screen.getByText("Recharge gratuite le dimanche, de 00h00 à 23h59 (heure du Gabon).")).toBeInTheDocument();
  });

  it("says it is loading while the window is unknown and offers no edit", () => {
    render(<WindowCard window={null} onChanged={onChanged} />);

    expect(screen.getByText("Chargement du créneau…")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Modifier le créneau" })).not.toBeInTheDocument();
  });

  it("hides the edit button without free_charging:manage", () => {
    granted.manage = false;
    render(<WindowCard window={wholeSunday} onChanged={onChanged} />);

    expect(screen.queryByRole("button", { name: "Modifier le créneau" })).not.toBeInTheDocument();
  });

  it("keeps the whole day when saved unchanged and sends no end time", async () => {
    render(<WindowCard window={wholeSunday} onChanged={onChanged} />);
    await userEvent.click(screen.getByRole("button", { name: "Modifier le créneau" }));

    await userEvent.click(screen.getByRole("button", { name: "Enregistrer" }));

    await waitFor(() => expect(service.changeWindow).toHaveBeenCalledWith({ day: "SUNDAY", from: "00:00", until: null, endOfDay: true }));
    expect(onChanged).toHaveBeenCalled();
  });

  it("saves a bounded window on another day", async () => {
    render(<WindowCard window={wholeSunday} onChanged={onChanged} />);
    await userEvent.click(screen.getByRole("button", { name: "Modifier le créneau" }));

    await userEvent.selectOptions(screen.getByLabelText("Jour *"), "SATURDAY");
    fireEvent.change(screen.getByLabelText("Début *"), { target: { value: "09:00" } });
    await userEvent.click(screen.getByRole("checkbox", { name: /fin de la journée/ }));
    fireEvent.change(screen.getByLabelText("Fin"), { target: { value: "17:30" } });
    await userEvent.click(screen.getByRole("button", { name: "Enregistrer" }));

    await waitFor(() => expect(service.changeWindow).toHaveBeenCalledWith({ day: "SATURDAY", from: "09:00", until: "17:30", endOfDay: false }));
  });

  it("refuses an end that is not after the start and does not call the API", async () => {
    render(<WindowCard window={wholeSunday} onChanged={onChanged} />);
    await userEvent.click(screen.getByRole("button", { name: "Modifier le créneau" }));
    await userEvent.click(screen.getByRole("checkbox", { name: /fin de la journée/ }));
    fireEvent.change(screen.getByLabelText("Début *"), { target: { value: "12:00" } });
    fireEvent.change(screen.getByLabelText("Fin"), { target: { value: "10:00" } });

    await userEvent.click(screen.getByRole("button", { name: "Enregistrer" }));

    expect(await screen.findByText("La fin doit venir après le début")).toBeInTheDocument();
    expect(service.changeWindow).not.toHaveBeenCalled();
  });

  it("shows the mapped message and stays open when the server refuses", async () => {
    service.changeWindow.mockRejectedValue(new ApiException({ timestamp: "", status: 422, code: "FREE_WINDOW_INVALID", message: "raw server text", path: "/x" }));
    render(<WindowCard window={wholeSunday} onChanged={onChanged} />);
    await userEvent.click(screen.getByRole("button", { name: "Modifier le créneau" }));

    await userEvent.click(screen.getByRole("button", { name: "Enregistrer" }));

    expect(await screen.findByText("Le créneau n'est pas valide : la fin doit venir après le début.")).toBeInTheDocument();
    expect(screen.queryByText("raw server text")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Enregistrer" })).toBeInTheDocument();
    expect(onChanged).not.toHaveBeenCalled();
  });
});
