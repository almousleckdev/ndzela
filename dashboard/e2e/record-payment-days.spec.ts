import { expect, test } from "@playwright/test";
import { account, driver, operators } from "./support/driver-fixtures";
import { open, signedIn } from "./support/fixtures";
import { MockApi } from "./support/mock-api";

const STAFF = ["drivers:read", "payments:read", "payments:create", "contracts:read"];
const payableDays = [
  { day: "2026-10-15", dueAt: "2026-10-15T12:00:00+01:00", rent: 23000, penalty: 0, amount: 23000, late: true },
  { day: "2026-10-16", dueAt: "2026-10-16T12:00:00+01:00", rent: 23000, penalty: 5000, amount: 28000, late: true },
  { day: "2026-10-17", dueAt: "2026-10-17T12:00:00+01:00", rent: 23000, penalty: 0, amount: 23000, late: true },
];

function driverPage() {
  return signedIn(new MockApi(), STAFF)
    .on("GET /drivers/driver-1", driver)
    .on("GET /payments/accounts/driver-1", { ...account, payableDays })
    .on("GET /payments/operators", operators)
    .on("POST /payments", { entry: { id: "e1", amount: 46000 }, level: "YELLOW", overdueAmount: 28000, credit: 0 });
}

test.describe("paying chosen days from Encaisser", () => {
  test("sends the ticked days, skipping an older one, and no amount", async ({ page }) => {
    const api = driverPage();
    await open(page, api, "/drivers/driver-1");

    await page.getByRole("button", { name: "Enregistrer un paiement" }).click();
    await expect(page.getByRole("checkbox")).toHaveCount(3);
    await page.getByRole("checkbox").nth(0).check();
    await page.getByRole("checkbox").nth(2).check();
    await expect(page.getByRole("dialog").getByText(/Total/)).toContainText("46");
    await page.getByLabel("Numéro de la transaction *").fill("PAY-DAYS-1");
    await page.getByRole("button", { name: "Enregistrer", exact: true }).click();

    await expect(page.getByText("Paiement enregistré")).toBeVisible();
    const sent = api.requestsTo("POST /payments");
    expect(sent).toHaveLength(1);
    expect(sent[0].body).toMatchObject({ driverId: "driver-1", days: ["2026-10-15", "2026-10-17"], method: "AIRTEL_MONEY", reference: "PAY-DAYS-1" });
    expect(sent[0].body).not.toHaveProperty("amount");
  });

  test("asks for at least one day and sends nothing", async ({ page }) => {
    const api = driverPage();
    await open(page, api, "/drivers/driver-1");

    await page.getByRole("button", { name: "Enregistrer un paiement" }).click();
    await page.getByLabel("Numéro de la transaction *").fill("PAY-DAYS-2");
    await page.getByRole("button", { name: "Enregistrer", exact: true }).click();

    await expect(page.getByRole("dialog").getByRole("alert")).toHaveText("Choisissez au moins un jour à encaisser.");
    expect(api.requestsTo("POST /payments")).toHaveLength(0);
  });

  test("switches to an amount for a partial payment", async ({ page }) => {
    const api = driverPage();
    await open(page, api, "/drivers/driver-1");

    await page.getByRole("button", { name: "Enregistrer un paiement" }).click();
    await page.getByRole("button", { name: "Saisir un montant" }).click();
    await page.getByLabel("Montant reçu (FCFA) *").fill("5000");
    await page.getByLabel("Numéro de la transaction *").fill("PAY-DAYS-3");
    await page.getByRole("button", { name: "Enregistrer", exact: true }).click();

    await expect(page.getByText("Paiement enregistré")).toBeVisible();
    const sent = api.requestsTo("POST /payments");
    expect(sent[0].body).toMatchObject({ amount: 5000 });
    expect(sent[0].body).not.toHaveProperty("days");
  });
});
