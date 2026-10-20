import { expect, test } from "@playwright/test";
import { account, driver, operators } from "./support/driver-fixtures";
import { open, signedIn } from "./support/fixtures";
import { failure, MockApi } from "./support/mock-api";

const STAFF = ["drivers:read", "payments:read", "payments:create", "contracts:read"];

function driverPage(permissions = STAFF) {
  return signedIn(new MockApi(), permissions)
    .on("GET /drivers/driver-1", driver)
    .on("GET /payments/accounts/driver-1", account)
    .on("GET /payments/operators", operators);
}

test.describe("recording a payment from the driver's page", () => {
  test("shows the contract progress since day one", async ({ page }) => {
    await open(page, driverPage(), "/drivers/driver-1");

    await expect(page.getByRole("heading", { name: "Suivi du contrat" })).toBeVisible();
    await expect(page.getByText(/Jour 12 sur 1\s?248/)).toBeVisible();
    await expect(page.getByRole("progressbar", { name: "Avancement du contrat" })).toBeVisible();
    await expect(page.getByText("Reste à régler").locator("xpath=following-sibling::dd")).toContainText("28");
  });

  test("sends the amount, the operator and the transaction number once", async ({ page }) => {
    const api = driverPage().on("POST /payments", { entry: { id: "e1" }, level: "NONE", overdueAmount: 0, credit: 0 });
    await open(page, api, "/drivers/driver-1");

    await page.getByRole("button", { name: "Enregistrer un paiement" }).click();
    await expect(page.getByRole("dialog")).toBeVisible();
    await expect(page.getByLabel("Montant reçu (FCFA) *")).toHaveValue("69000");
    await page.getByLabel("Numéro de la transaction *").fill("PAY0110261325109");
    await page.getByRole("button", { name: "Enregistrer", exact: true }).click();

    await expect(page.getByText("Paiement enregistré")).toBeVisible();
    const sent = api.requestsTo("POST /payments");
    expect(sent).toHaveLength(1);
    expect(sent[0].body).toMatchObject({ driverId: "driver-1", amount: 69000, method: "AIRTEL_MONEY", reference: "PAY0110261325109" });
    expect((sent[0].body as { requestId: string }).requestId).toMatch(/^[0-9a-f-]{36}$/);
  });

  test("refuses a payment without its transaction number and sends nothing", async ({ page }) => {
    const api = driverPage();
    await open(page, api, "/drivers/driver-1");

    await page.getByRole("button", { name: "Enregistrer un paiement" }).click();
    await page.getByRole("button", { name: "Enregistrer", exact: true }).click();

    await expect(page.getByRole("dialog").getByRole("alert")).toHaveText("Le numéro de la transaction est obligatoire.");
    expect(api.requestsTo("POST /payments")).toHaveLength(0);
  });

  test("shows the server's reason when the payment is refused", async ({ page }) => {
    const api = driverPage().on("POST /payments", failure(422, "ENTRY_ALREADY_REVERSED"));
    await open(page, api, "/drivers/driver-1");

    await page.getByRole("button", { name: "Enregistrer un paiement" }).click();
    await page.getByLabel("Numéro de la transaction *").fill("PAY-1");
    await page.getByRole("button", { name: "Enregistrer", exact: true }).click();

    await expect(page.getByRole("dialog").getByRole("alert")).toHaveText("Cette écriture a déjà été annulée.");
    await expect(page.getByRole("dialog")).toBeVisible();
  });

  test("hides the button from staff who may only read payments", async ({ page }) => {
    await open(page, driverPage(["drivers:read", "payments:read"]), "/drivers/driver-1");

    await expect(page.getByRole("heading", { name: "Suivi du contrat" })).toBeVisible();
    await expect(page.getByRole("button", { name: "Enregistrer un paiement" })).toHaveCount(0);
  });
});
