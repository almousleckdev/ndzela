import { expect, test } from "@playwright/test";
import { open, page0, signedIn } from "./support/fixtures";
import { failure, MockApi } from "./support/mock-api";

const COUNTS = { AVAILABLE: 0, ASSIGNED: 0, IN_MAINTENANCE: 0, ACCIDENT: 0, SUSPENDED: 0, OUT_OF_SERVICE: 0 };

function vehiclesPage(permissions = ["vehicles:read", "vehicles:create"]) {
  return signedIn(new MockApi(), permissions).on("GET /vehicles", page0()).on("GET /vehicles/counts", COUNTS);
}

async function fillVehicle(page: import("@playwright/test").Page) {
  await page.getByLabel("Numéro du véhicule *").fill("V-30");
  await page.getByLabel("Immatriculation *").fill("LZ-202-BB");
  await page.getByLabel("Marque *").fill("Dongfeng");
  await page.getByLabel("Modèle *").fill("Box");
  await page.getByLabel("Année *").fill("2025");
  await page.getByRole("button", { name: "Bleu" }).click();
  await page.getByLabel("Prix d'achat (FCFA) *").fill("13000000");
  await page.getByLabel("Loyer journalier (FCFA) *").fill("23000");
}

test.describe("adding a vehicle", () => {
  test("is checked in a recap, then sent with numbers as numbers", async ({ page }) => {
    const api = vehiclesPage().on("POST /vehicles", { id: "vehicle-2" });
    await open(page, api, "/vehicles");

    await page.getByRole("button", { name: "Ajouter un véhicule" }).click();
    await fillVehicle(page);
    await page.getByRole("button", { name: "Continuer" }).click();

    await expect(page.getByRole("heading", { name: "Récapitulatif" })).toBeVisible();
    expect(api.requestsTo("POST /vehicles")).toHaveLength(0);
    await page.getByRole("button", { name: "Enregistrer le véhicule" }).click();

    await expect(page.getByText("Véhicule ajouté.")).toBeVisible();
    expect(api.requestsTo("POST /vehicles")[0].body).toEqual({
      fleetNumber: "V-30", brand: "Dongfeng", model: "Box", year: 2025, plate: "LZ-202-BB", color: "#2563EB", vin: null, purchasePrice: 13000000, dailyRent: 23000,
    });
  });

  test("shows what is missing and sends nothing", async ({ page }) => {
    const api = vehiclesPage();
    await open(page, api, "/vehicles");

    await page.getByRole("button", { name: "Ajouter un véhicule" }).click();
    await page.getByRole("button", { name: "Continuer" }).click();

    await expect(page.getByText("Le numéro du véhicule est requis")).toBeVisible();
    await expect(page.getByText("Montant invalide (chiffres uniquement)").first()).toBeVisible();
    expect(api.requestsTo("POST /vehicles")).toHaveLength(0);
  });

  test("goes back to the form with the server's reason when the plate is taken", async ({ page }) => {
    const api = vehiclesPage().on("POST /vehicles", failure(422, "VEHICLE_PLATE_TAKEN"));
    await open(page, api, "/vehicles");

    await page.getByRole("button", { name: "Ajouter un véhicule" }).click();
    await fillVehicle(page);
    await page.getByRole("button", { name: "Continuer" }).click();
    await page.getByRole("button", { name: "Enregistrer le véhicule" }).click();

    await expect(page.getByText("Cette immatriculation est déjà utilisée.")).toBeVisible();
    await expect(page.getByLabel("Immatriculation *")).toHaveValue("LZ-202-BB");
  });

  test("offers no add button without the permission", async ({ page }) => {
    await open(page, vehiclesPage(["vehicles:read"]), "/vehicles");

    await expect(page.getByRole("heading", { name: /Véhicules|Flotte/ }).first()).toBeVisible();
    await expect(page.getByRole("button", { name: "Ajouter un véhicule" })).toHaveCount(0);
  });
});
