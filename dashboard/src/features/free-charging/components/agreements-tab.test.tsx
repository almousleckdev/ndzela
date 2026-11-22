import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiException } from "@/lib/api/api-error";
import { AgreementsTab } from "@/features/free-charging/components/agreements-tab";
import type { Agreement } from "@/features/free-charging/types";

const service = vi.hoisted(() => ({ list: vi.fn(), create: vi.fn(), update: vi.fn(), suspend: vi.fn(), resume: vi.fn(), revoke: vi.fn(), drivers: vi.fn() }));
const toast = vi.hoisted(() => vi.fn());
const granted = vi.hoisted(() => ({ manage: true }));

vi.mock("@/features/free-charging/services/free-charging.service", () => ({ freeChargingService: service }));
vi.mock("@/hooks/use-toast", () => ({ useToast: () => ({ addToast: toast }) }));
vi.mock("@/features/access/hooks/use-permissions", () => ({ usePermissions: () => ({ can: (permission: string) => permission === "free_charging:read" || (granted.manage && permission === "free_charging:manage") }) }));

const agreement = (overrides: Partial<Agreement>): Agreement => ({
  id: "a1", driverId: "d1", driverName: "Paul Aba", startsOn: "2026-11-01", endsOn: "2026-12-31", status: "ACTIVE", conditions: "Taxi de nuit",
  eligibility: "UNPAID_DAYS", unpaidDays: 3, lastFreeUse: null, ...overrides,
});
const DRIVERS = [
  { driverId: "d-jean", customerId: "c-jean", fullName: "MBA Jean", phone: "+24177000001", email: "jean@example.com", fleetNumber: "V015", plate: "PL-0150", status: "ACTIVE", linked: true, inProgram: false, agreementId: null, agreementStatus: null },
  { driverId: "d-paul", customerId: "c-paul", fullName: "NDONG Paul", phone: "+24166000002", email: null, fleetNumber: "V030", plate: "PL-0300", status: "ACTIVE", linked: true, inProgram: false, agreementId: null, agreementStatus: null },
  { driverId: "d-awa", customerId: "c-awa", fullName: "OBIANG Awa", phone: null, email: null, fleetNumber: null, plate: null, status: "ACTIVE", linked: true, inProgram: true, agreementId: "ag-awa", agreementStatus: "ACTIVE" },
  { driverId: "d-zoe", customerId: "c-zoe", fullName: "ZOGO Zoé", phone: null, email: null, fleetNumber: null, plate: null, status: "ARCHIVED", linked: true, inProgram: false, agreementId: null, agreementStatus: null },
  { driverId: "d-luc", customerId: null, fullName: "ONDO Luc", phone: "+24162000004", email: null, fleetNumber: "V041", plate: "PL-0410", status: "ACTIVE", linked: false, inProgram: false, agreementId: null, agreementStatus: null },
];
const rows = [agreement({}), agreement({ id: "a2", driverName: "Awa Nze", status: "SUSPENDED", eligibility: "OUTSIDE_WINDOW", unpaidDays: 0 })];

beforeEach(() => {
  vi.clearAllMocks();
  granted.manage = true;
  service.list.mockResolvedValue(rows);
  service.drivers.mockImplementation((q: string) => Promise.resolve({ items: q === "zzz" ? [] : DRIVERS, page: 0, size: 25, total: q === "zzz" ? 0 : DRIVERS.length }));
});

describe("AgreementsTab table", () => {
  it("shows the status, the eligibility label and the unpaid days from the API", async () => {
    render(<AgreementsTab adding={false} onAddingClose={() => {}} />);

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
    render(<AgreementsTab adding={false} onAddingClose={() => {}} />);
    await screen.findByText("Paul Aba");

    for (const name of [/^Modifier/, /^Suspendre/, /^Reprendre/, /^Retirer/]) expect(screen.queryByRole("button", { name })).not.toBeInTheDocument();
  });

  it("shows the management buttons with free_charging:manage", async () => {
    render(<AgreementsTab adding={false} onAddingClose={() => {}} />);
    await screen.findByText("Paul Aba");

    expect(screen.getAllByRole("button", { name: /^Modifier/ })).toHaveLength(2);
    expect(screen.getByRole("button", { name: "Suspendre Paul Aba" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Reprendre Awa Nze" })).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: /^Retirer/ })).toHaveLength(2);
  });

  it("shows the empty state", async () => {
    service.list.mockResolvedValue([]);
    render(<AgreementsTab adding={false} onAddingClose={() => {}} />);

    expect(await screen.findByText("Aucun chauffeur dans le programme")).toBeInTheDocument();
  });
});

describe("AgreementsTab actions", () => {
  it("suspends and resumes through the service", async () => {
    service.suspend.mockResolvedValue({});
    service.resume.mockResolvedValue({});
    render(<AgreementsTab adding={false} onAddingClose={() => {}} />);
    await screen.findByText("Paul Aba");

    fireEvent.click(screen.getByRole("button", { name: "Suspendre Paul Aba" }));
    await waitFor(() => expect(service.suspend).toHaveBeenCalledWith("a1"));
    fireEvent.click(screen.getByRole("button", { name: "Reprendre Awa Nze" }));
    await waitFor(() => expect(service.resume).toHaveBeenCalledWith("a2"));
  });

  it("asks for confirmation before revoking and says the history is kept", async () => {
    service.revoke.mockResolvedValue({});
    render(<AgreementsTab adding={false} onAddingClose={() => {}} />);
    await screen.findByText("Paul Aba");

    fireEvent.click(screen.getByRole("button", { name: "Retirer Paul Aba" }));
    expect(service.revoke).not.toHaveBeenCalled();
    expect(screen.getByText(/L'historique de l'accord est conservé/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /^Retirer$/ }));

    await waitFor(() => expect(service.revoke).toHaveBeenCalledWith("a1"));
  });

  it("does not revoke when the confirmation is cancelled", async () => {
    render(<AgreementsTab adding={false} onAddingClose={() => {}} />);
    await screen.findByText("Paul Aba");

    fireEvent.click(screen.getByRole("button", { name: "Retirer Paul Aba" }));
    fireEvent.click(screen.getByRole("button", { name: "Annuler" }));

    expect(service.revoke).not.toHaveBeenCalled();
  });

  it("keeps the revoke dialog open with the mapped message when revoking fails", async () => {
    service.revoke.mockRejectedValue(new ApiException({ timestamp: "", status: 422, code: "FREE_AGREEMENT_REVOKED", message: "english", path: "/x" }));
    render(<AgreementsTab adding={false} onAddingClose={() => {}} />);
    await screen.findByText("Paul Aba");

    fireEvent.click(screen.getByRole("button", { name: "Retirer Paul Aba" }));
    fireEvent.click(screen.getByRole("button", { name: /^Retirer$/ }));

    expect(await screen.findByText(/Cet accord est retiré et ne peut plus être modifié/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /^Retirer$/ })).toBeInTheDocument();
  });

  it("edits the dates and conditions through the service", async () => {
    service.update.mockResolvedValue({});
    render(<AgreementsTab adding={false} onAddingClose={() => {}} />);
    await screen.findByText("Paul Aba");

    fireEvent.click(screen.getByRole("button", { name: "Modifier Paul Aba" }));
    fireEvent.change(screen.getByLabelText("Fin (facultatif)"), { target: { value: "2027-01-31" } });
    fireEvent.click(screen.getByRole("button", { name: "Enregistrer" }));

    await waitFor(() => expect(service.update).toHaveBeenCalledWith("a1", { startsOn: "2026-11-01", endsOn: "2027-01-31", conditions: "Taxi de nuit" }));
  });
});


const SAVE = { name: "Enregistrer les changements" };

describe("Add form", () => {
  const open = async () => {
    render(<AgreementsTab adding onAddingClose={() => {}} />);
    await screen.findByText("Paul Aba");
    return screen.getByRole("dialog");
  };
  const pick = async (dialog: HTMLElement, name: RegExp) => userEvent.click(await within(dialog).findByRole("checkbox", { name }));
  const created = () => service.create.mock.calls.map(([driverId]) => driverId);

  it("lists every driver in a table with car, status and place in the program", async () => {
    const dialog = await open();

    const table = await within(dialog).findByRole("table");
    expect(within(table).getAllByRole("row")).toHaveLength(DRIVERS.length + 1);
    expect(within(table).getByText("V015 · PL-0150")).toBeInTheDocument();
    expect(within(table).getByText("Démissionnaire")).toBeInTheDocument();
    expect(within(table).getByText("Dans le programme")).toBeInTheDocument();
    expect(within(table).getByText("Compte Zella non lié")).toBeInTheDocument();
  });

  it("shows a driver in the program ticked, the others unticked, and blocks who cannot be added", async () => {
    const dialog = await open();

    expect(await within(dialog).findByRole("checkbox", { name: /OBIANG Awa/ })).toBeChecked();
    expect(within(dialog).getByRole("checkbox", { name: /MBA Jean/ })).not.toBeChecked();
    expect(within(dialog).getByRole("checkbox", { name: /ONDO Luc/ })).toBeDisabled();
    expect(within(dialog).getByRole("checkbox", { name: /ZOGO/ })).toBeDisabled();
  });

  it("asks for at least one change", async () => {
    const dialog = await open();
    await within(dialog).findByRole("table");
    fireEvent.click(within(dialog).getByRole("button", SAVE));

    expect(await within(dialog).findByText("Cochez ou décochez au moins un chauffeur.")).toBeInTheDocument();
    expect(service.create).not.toHaveBeenCalled();
  });

  it("asks for the dates once a driver is ticked, and refuses an end before the start", async () => {
    const dialog = await open();
    await pick(dialog, /MBA Jean/);
    fireEvent.change(within(dialog).getByLabelText("Début"), { target: { value: "2026-11-10" } });
    fireEvent.change(within(dialog).getByLabelText("Fin (facultatif)"), { target: { value: "2026-11-01" } });
    fireEvent.click(within(dialog).getByRole("button", SAVE));

    expect(await within(dialog).findByText("La date de fin ne peut pas précéder la date de début")).toBeInTheDocument();
    expect(service.create).not.toHaveBeenCalled();
  });

  it("explains what an empty end date means", async () => {
    const dialog = await open();
    await pick(dialog, /MBA Jean/);

    expect(within(dialog).getByText("Sans date de fin, l'accord reste actif jusqu'à ce que vous le suspendiez ou le retiriez.")).toBeInTheDocument();
  });

  it("creates the agreement for a ticked driver", async () => {
    service.create.mockResolvedValue({});
    const dialog = await open();
    await pick(dialog, /MBA Jean/);
    fireEvent.change(within(dialog).getByLabelText("Début"), { target: { value: "2026-11-10" } });
    fireEvent.click(within(dialog).getByRole("button", SAVE));

    await waitFor(() => expect(service.create).toHaveBeenCalledWith("d-jean", { startsOn: "2026-11-10", endsOn: null, conditions: null }));
  });

  it("removes a driver from the program by unticking them, without asking for dates", async () => {
    service.revoke.mockResolvedValue({});
    const dialog = await open();
    await pick(dialog, /OBIANG Awa/);

    expect(within(dialog).queryByLabelText("Début")).not.toBeInTheDocument();
    expect(within(dialog).getByText("Sera retiré")).toBeInTheDocument();
    fireEvent.click(within(dialog).getByRole("button", SAVE));

    await waitFor(() => expect(service.revoke).toHaveBeenCalledWith("ag-awa"));
    expect(service.create).not.toHaveBeenCalled();
  });

  it("adds and removes in one save", async () => {
    service.create.mockResolvedValue({});
    service.revoke.mockResolvedValue({});
    const dialog = await open();
    await pick(dialog, /MBA Jean/);
    await pick(dialog, /NDONG Paul/);
    await pick(dialog, /OBIANG Awa/);
    expect(within(dialog).getByText("2 à ajouter · 1 à retirer")).toBeInTheDocument();
    fireEvent.change(within(dialog).getByLabelText("Début"), { target: { value: "2026-11-10" } });
    fireEvent.click(within(dialog).getByRole("button", SAVE));

    await waitFor(() => expect(created()).toEqual(["d-jean", "d-paul"]));
    expect(service.revoke).toHaveBeenCalledTimes(1);
  });

  it("ticks the whole page with one click, never the blocked ones, and cancels them", async () => {
    const dialog = await open();
    await within(dialog).findByRole("table");

    await userEvent.click(within(dialog).getByRole("checkbox", { name: "Sélectionner les chauffeurs de la page" }));
    expect(within(dialog).getByRole("checkbox", { name: /MBA Jean/ })).toBeChecked();
    expect(within(dialog).getByRole("checkbox", { name: /NDONG Paul/ })).toBeChecked();
    expect(within(dialog).getByRole("checkbox", { name: /ONDO Luc/ })).not.toBeChecked();
    await userEvent.click(within(dialog).getByRole("button", { name: "Annuler les changements" }));

    expect(within(dialog).getByRole("checkbox", { name: /MBA Jean/ })).not.toBeChecked();
    expect(within(dialog).getByRole("checkbox", { name: /OBIANG Awa/ })).toBeChecked();
  });

  it("searches the drivers on the typed text, a car number included", async () => {
    const dialog = await open();
    await within(dialog).findByRole("checkbox", { name: /MBA Jean/ });

    fireEvent.change(within(dialog).getByRole("searchbox"), { target: { value: "V015" } });

    await waitFor(() => expect(service.drivers).toHaveBeenCalledWith("V015", "ALL", 0));
  });

  it("filters on who is in the program", async () => {
    const dialog = await open();
    await within(dialog).findByRole("table");

    await userEvent.click(within(dialog).getByRole("button", { name: "Dans le programme" }));

    await waitFor(() => expect(service.drivers).toHaveBeenCalledWith("", "IN_PROGRAM", 0));
  });

  it("keeps the ticks when the page changes", async () => {
    service.create.mockResolvedValue({});
    service.drivers.mockImplementation((q: string, membership: string, page: number) => Promise.resolve({ items: [DRIVERS[page === 0 ? 0 : 1]], page, size: 25, total: 30 }));
    const dialog = await open();
    await pick(dialog, /MBA Jean/);

    await userEvent.click(within(dialog).getByRole("button", { name: "Suivant" }));
    await pick(dialog, /NDONG Paul/);
    expect(within(dialog).getByText("2 à ajouter · 0 à retirer")).toBeInTheDocument();
    fireEvent.change(within(dialog).getByLabelText("Début"), { target: { value: "2026-11-10" } });
    fireEvent.click(within(dialog).getByRole("button", SAVE));

    await waitFor(() => expect(created()).toEqual(["d-jean", "d-paul"]));
  });

  it("lists the changes that failed and retries only those", async () => {
    let paulFails = true;
    service.create.mockImplementation((driverId: string) => (driverId === "d-paul" && paulFails
      ? Promise.reject(new ApiException({ timestamp: "", status: 422, code: "FREE_AGREEMENT_EXISTS", message: "english", path: "/x" })) : Promise.resolve({})));
    const dialog = await open();
    await pick(dialog, /MBA Jean/);
    await pick(dialog, /NDONG Paul/);
    fireEvent.change(within(dialog).getByLabelText("Début"), { target: { value: "2026-11-10" } });

    fireEvent.click(within(dialog).getByRole("button", SAVE));

    expect(await within(dialog).findByText("1 chauffeur ajouté.")).toBeInTheDocument();
    expect(within(dialog).getByText("NDONG Paul : Ce chauffeur a déjà un accord de gratuité. Retirez-le d'abord.")).toBeInTheDocument();
    paulFails = false;
    fireEvent.click(within(dialog).getByRole("button", { name: "Réessayer" }));
    await waitFor(() => expect(created()).toEqual(["d-jean", "d-paul", "d-paul"]));
  });
});

describe("List search and figures", () => {
  it("counts the drivers in the program and filters the list by name, phone, e-mail or car", async () => {
    service.list.mockResolvedValue([
      agreement({ fleetNumber: "V015", plate: "PL-0150", phone: "+24177001122", eligibility: "ELIGIBLE" }),
      agreement({ id: "a2", driverName: "Awa Nze", fleetNumber: "V030", plate: "PL-0300", phone: "+24166002233", eligibility: "UNPAID_DAYS" }),
      agreement({ id: "a3", driverName: "Luc Ondo", status: "REVOKED", eligibility: "NO_AGREEMENT" }),
    ]);
    render(<AgreementsTab adding={false} onAddingClose={() => {}} />);
    await screen.findByText("Paul Aba");

    expect(screen.getByText("Dans le programme").parentElement).toHaveTextContent("2");
    expect(screen.getByText("Éligibles maintenant").parentElement).toHaveTextContent("1");
    expect(screen.getByText("À régulariser").parentElement).toHaveTextContent("1");
    expect(screen.getByText("V015 · PL-0150")).toBeInTheDocument();

    fireEvent.change(screen.getByRole("searchbox"), { target: { value: "V030" } });
    await waitFor(() => expect(screen.queryByText("Paul Aba")).not.toBeInTheDocument());
    expect(screen.getByText("Awa Nze")).toBeInTheDocument();
    fireEvent.change(screen.getByRole("searchbox"), { target: { value: "077 00 11 22" } });
    await waitFor(() => expect(screen.queryByText("Awa Nze")).not.toBeInTheDocument());
    expect(screen.getByText("Paul Aba")).toBeInTheDocument();
    fireEvent.change(screen.getByRole("searchbox"), { target: { value: "zzzz" } });
    expect(await screen.findByText("Aucun chauffeur ne correspond à cette recherche.")).toBeInTheDocument();
  });
});
