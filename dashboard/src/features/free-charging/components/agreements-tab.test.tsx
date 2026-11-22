import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiException } from "@/lib/api/api-error";
import { AgreementsTab } from "@/features/free-charging/components/agreements-tab";
import type { Agreement } from "@/features/free-charging/types";

const service = vi.hoisted(() => ({ list: vi.fn(), create: vi.fn(), update: vi.fn(), suspend: vi.fn(), resume: vi.fn(), revoke: vi.fn() }));
const toast = vi.hoisted(() => vi.fn());
const granted = vi.hoisted(() => ({ manage: true }));

vi.mock("@/features/free-charging/services/free-charging.service", () => ({ freeChargingService: service }));
const driversList = vi.hoisted(() => vi.fn());
vi.mock("@/features/drivers/services/drivers.service", () => ({ driversService: { list: driversList } }));
vi.mock("@/hooks/use-toast", () => ({ useToast: () => ({ addToast: toast }) }));
vi.mock("@/features/access/hooks/use-permissions", () => ({ usePermissions: () => ({ can: (permission: string) => permission === "free_charging:read" || (granted.manage && permission === "free_charging:manage") }) }));

const agreement = (overrides: Partial<Agreement>): Agreement => ({
  id: "a1", driverId: "d1", driverName: "Paul Aba", startsOn: "2026-11-01", endsOn: "2026-12-31", status: "ACTIVE", conditions: "Taxi de nuit",
  eligibility: "UNPAID_DAYS", unpaidDays: 3, lastFreeUse: null, ...overrides,
});
const rows = [agreement({}), agreement({ id: "a2", driverName: "Awa Nze", status: "SUSPENDED", eligibility: "OUTSIDE_WINDOW", unpaidDays: 0 })];

beforeEach(() => {
  vi.clearAllMocks();
  granted.manage = true;
  service.list.mockResolvedValue(rows);
  driversList.mockImplementation(({ q }: { q: string }) => Promise.resolve({ items: q === "zzz" ? [] : [{ id: "d9", personal: { lastName: "Nze", firstName: "Awa" } }], page: 0, size: 20, total: 1 }));
});

describe("AgreementsTab table", () => {
  it("shows the status, the eligibility label and the unpaid days from the API", async () => {
    render(<AgreementsTab />);

    const first = (await screen.findByText("Paul Aba")).closest("tr") as HTMLElement;
    expect(within(first).getByText("Actif")).toBeInTheDocument();
    expect(within(first).getByText("Jours impayés", { selector: "td *, td" })).toBeInTheDocument();
    expect(within(first).getByText("3")).toBeInTheDocument();
    const second = screen.getByText("Awa Nze").closest("tr") as HTMLElement;
    expect(within(second).getByText("Suspendu")).toBeInTheDocument();
    expect(within(second).getByText("Hors créneau")).toBeInTheDocument();
  });

  it("hides every management button without free_charging:manage", async () => {
    granted.manage = false;
    render(<AgreementsTab />);
    await screen.findByText("Paul Aba");

    expect(screen.queryByRole("button", { name: "Ajouter un chauffeur" })).not.toBeInTheDocument();
    for (const name of [/^Modifier/, /^Suspendre/, /^Reprendre/, /^Retirer/]) expect(screen.queryByRole("button", { name })).not.toBeInTheDocument();
  });

  it("shows the management buttons with free_charging:manage", async () => {
    render(<AgreementsTab />);
    await screen.findByText("Paul Aba");

    expect(screen.getByRole("button", { name: "Ajouter un chauffeur" })).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: /^Modifier/ })).toHaveLength(2);
    expect(screen.getByRole("button", { name: "Suspendre Paul Aba" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Reprendre Awa Nze" })).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: /^Retirer/ })).toHaveLength(2);
  });

  it("shows the empty state", async () => {
    service.list.mockResolvedValue([]);
    render(<AgreementsTab />);

    expect(await screen.findByText("Aucun chauffeur")).toBeInTheDocument();
  });
});

describe("AgreementsTab actions", () => {
  it("suspends and resumes through the service", async () => {
    service.suspend.mockResolvedValue({});
    service.resume.mockResolvedValue({});
    render(<AgreementsTab />);
    await screen.findByText("Paul Aba");

    fireEvent.click(screen.getByRole("button", { name: "Suspendre Paul Aba" }));
    await waitFor(() => expect(service.suspend).toHaveBeenCalledWith("a1"));
    fireEvent.click(screen.getByRole("button", { name: "Reprendre Awa Nze" }));
    await waitFor(() => expect(service.resume).toHaveBeenCalledWith("a2"));
  });

  it("asks for confirmation before revoking and says the history is kept", async () => {
    service.revoke.mockResolvedValue({});
    render(<AgreementsTab />);
    await screen.findByText("Paul Aba");

    fireEvent.click(screen.getByRole("button", { name: "Retirer Paul Aba" }));
    expect(service.revoke).not.toHaveBeenCalled();
    expect(screen.getByText(/L'historique est conservé/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Retirer l'accord" }));

    await waitFor(() => expect(service.revoke).toHaveBeenCalledWith("a1"));
  });

  it("does not revoke when the confirmation is cancelled", async () => {
    render(<AgreementsTab />);
    await screen.findByText("Paul Aba");

    fireEvent.click(screen.getByRole("button", { name: "Retirer Paul Aba" }));
    fireEvent.click(screen.getByRole("button", { name: "Annuler" }));

    expect(service.revoke).not.toHaveBeenCalled();
  });

  it("keeps the revoke dialog open with the mapped message when revoking fails", async () => {
    service.revoke.mockRejectedValue(new ApiException({ timestamp: "", status: 422, code: "FREE_AGREEMENT_REVOKED", message: "english", path: "/x" }));
    render(<AgreementsTab />);
    await screen.findByText("Paul Aba");

    fireEvent.click(screen.getByRole("button", { name: "Retirer Paul Aba" }));
    fireEvent.click(screen.getByRole("button", { name: "Retirer l'accord" }));

    expect(await screen.findByText(/Cet accord est retiré et ne peut plus être modifié/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Retirer l'accord" })).toBeInTheDocument();
  });

  it("edits the dates and conditions through the service", async () => {
    service.update.mockResolvedValue({});
    render(<AgreementsTab />);
    await screen.findByText("Paul Aba");

    fireEvent.click(screen.getByRole("button", { name: "Modifier Paul Aba" }));
    fireEvent.change(screen.getByLabelText("Date de fin"), { target: { value: "2027-01-31" } });
    fireEvent.click(screen.getByRole("button", { name: "Enregistrer" }));

    await waitFor(() => expect(service.update).toHaveBeenCalledWith("a1", { startsOn: "2026-11-01", endsOn: "2027-01-31", conditions: "Taxi de nuit" }));
  });
});

describe("Add form", () => {
  const open = async () => {
    render(<AgreementsTab />);
    await screen.findByText("Paul Aba");
    fireEvent.click(screen.getByRole("button", { name: "Ajouter un chauffeur" }));
  };

  it("requires a driver and refuses an end before the start", async () => {
    await open();
    fireEvent.change(screen.getByLabelText("Date de début *"), { target: { value: "2026-11-10" } });
    fireEvent.change(screen.getByLabelText("Date de fin"), { target: { value: "2026-11-01" } });
    fireEvent.click(screen.getByRole("button", { name: "Enregistrer" }));

    expect(await screen.findByText("Choisissez un chauffeur")).toBeInTheDocument();
    expect(screen.getByText("La date de fin ne peut pas précéder la date de début")).toBeInTheDocument();
    expect(service.create).not.toHaveBeenCalled();
  });

  it("creates the agreement for the chosen driver", async () => {
    service.create.mockResolvedValue({});
    await open();
    await userEvent.selectOptions(await screen.findByLabelText("Chauffeur *"), "d9");
    fireEvent.change(screen.getByLabelText("Date de début *"), { target: { value: "2026-11-10" } });
    fireEvent.click(screen.getByRole("button", { name: "Enregistrer" }));

    await waitFor(() => expect(service.create).toHaveBeenCalledWith("d9", { startsOn: "2026-11-10", endsOn: null, conditions: null }));
  });

  it("keeps the chosen driver shown and submitted after the search changes", async () => {
    service.create.mockResolvedValue({});
    await open();
    await userEvent.selectOptions(await screen.findByLabelText("Chauffeur *"), "d9");
    fireEvent.change(screen.getByLabelText("Rechercher un chauffeur"), { target: { value: "zzz" } });
    await waitFor(() => expect(driversList).toHaveBeenCalledWith(expect.objectContaining({ q: "zzz" })));

    expect(await screen.findByRole("option", { name: "Nze Awa" })).toBeInTheDocument();
    expect((screen.getByLabelText("Chauffeur *") as HTMLSelectElement).value).toBe("d9");
    fireEvent.change(screen.getByLabelText("Date de début *"), { target: { value: "2026-11-10" } });
    fireEvent.click(screen.getByRole("button", { name: "Enregistrer" }));

    await waitFor(() => expect(service.create).toHaveBeenCalledWith("d9", expect.anything()));
  });

  it("shows the French message of FREE_AGREEMENT_EXISTS", async () => {
    service.create.mockRejectedValue(new ApiException({ timestamp: "", status: 422, code: "FREE_AGREEMENT_EXISTS", message: "english", path: "/x" }));
    await open();
    await userEvent.selectOptions(await screen.findByLabelText("Chauffeur *"), "d9");
    fireEvent.change(screen.getByLabelText("Date de début *"), { target: { value: "2026-11-10" } });
    fireEvent.click(screen.getByRole("button", { name: "Enregistrer" }));

    expect(await screen.findByText("Ce chauffeur a déjà un accord de gratuité. Retirez-le d'abord.")).toBeInTheDocument();
  });
});
