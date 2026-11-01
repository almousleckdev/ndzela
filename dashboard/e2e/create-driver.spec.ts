import { expect, test, type Page } from "@playwright/test";
import { open, page0, signedIn } from "./support/fixtures";
import { failure, MockApi } from "./support/mock-api";

const STAFF = ["drivers:read", "drivers:create", "vehicles:read"];
const VEHICLE = { id: "vehicle-1", fleetNumber: "V-29", brand: "Dongfeng", model: "Box", year: 2025, plate: "LZ-101-AA", color: "#2563EB", dailyRent: 23000, status: "AVAILABLE" };
const PHOTO = { name: "photo.png", mimeType: "image/png", buffer: Buffer.from("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==", "base64") };

function wizard() {
  return signedIn(new MockApi(), STAFF)
    .on("GET /training-centers", [{ id: "center-1", name: "NDZELA" }])
    .on("GET /vehicles", page0([VEHICLE]));
}

const field = (page: Page, name: string) => page.locator(`[name="${name}"]`);

async function fillPersonal(page: Page) {
  await page.getByLabel("Photo du chauffeur *").setInputFiles(PHOTO);
  await field(page, "personal.civility").selectOption("MALE");
  await field(page, "personal.birthDate").fill("1979-08-11");
  await field(page, "personal.lastName").fill("Khatim");
  await field(page, "personal.firstName").fill("Rachid Cedric");
  await field(page, "personal.birthPlace").fill("Bitam");
  await field(page, "personal.nationality").fill("Gabonaise");
  await field(page, "personal.idType").selectOption("NATIONAL_ID");
  await field(page, "personal.idNumber").fill("22PP11614");
  await field(page, "personal.idIssueDate").fill("2022-10-13");
  await field(page, "personal.idIssuePlace").fill("Libreville");
  await field(page, "personal.address").fill("Okala, derrière Ciciba");
  await field(page, "personal.district").fill("Okala");
  await field(page, "personal.city").fill("Libreville");
  await field(page, "personal.phonePrimary").fill("62143500");
  await field(page, "personal.email").fill("rachid@ndzela.test");
  await field(page, "personal.licenceNumber").fill("20191B03151");
  await field(page, "personal.licenceIssueDate").fill("2019-03-09");
  await field(page, "personal.licenceCategory").selectOption("B");
}

async function next(page: Page) {
  await page.getByRole("button", { name: "Suivant" }).click();
}

test.describe("creating a driver", () => {
  test("goes through the four steps and sends the driver, the contract and the files together", async ({ page }) => {
    const api = wizard().on("POST /drivers", { id: "driver-9", contract: { id: "contract-9" } });
    await open(page, api, "/drivers/new");

    await expect(page.getByText("Étape 1 sur 4")).toBeVisible();
    await fillPersonal(page);
    await next(page);

    await expect(page.getByText("Étape 2 sur 4")).toBeVisible();
    await field(page, "emergency.lastName").fill("Nang Ndong");
    await field(page, "emergency.firstName").fill("Brigitte");
    await field(page, "emergency.phone").fill("66685807");
    await field(page, "emergency.relationship").selectOption("MOTHER");
    await field(page, "emergency.address").fill("Akanda, Ciciba");
    await next(page);

    await expect(page.getByText("Étape 3 sur 4")).toBeVisible();
    await field(page, "vehicleId").selectOption("vehicle-1");
    await field(page, "training.trainingCenterId").selectOption("center-1");
    await field(page, "training.trainingDate").fill("2026-10-15");
    await next(page);

    await expect(page.getByText("Étape 4 sur 4")).toBeVisible();
    await expect(field(page, "contract.deposit")).toHaveValue("250000");
    await field(page, "contract.deposit").fill("500000");
    await page.getByRole("button", { name: "Continuer" }).click();

    await page.getByRole("dialog").getByLabel("Mot de passe", { exact: true }).fill("Str0ng!Pass");
    await page.getByRole("dialog").getByLabel("Confirmer le mot de passe").fill("Str0ng!Pass");
    await page.getByRole("button", { name: "Créer le chauffeur" }).click();

    await expect(page.getByText("Chauffeur créé.")).toBeVisible();
    const [sent] = api.requestsTo("POST /drivers");
    expect(sent.files).toEqual(["photo"]);
    expect(sent.body).toMatchObject({
      vehicleId: "vehicle-1", password: "Str0ng!Pass", contract: { deposit: 500000 },
      personal: { lastName: "Khatim", email: "rachid@ndzela.test", phonePrimary: "+241 62143500", phoneSecondary: null },
      emergency: { phone: "+241 66685807", relationship: "MOTHER" },
      training: { trainingCenterId: "center-1", certificateNumber: null },
    });
    await expect(page).toHaveURL(/\/contracts\/contract-9$/);
  });

  test("will not leave step 1 without the photo and the required fields", async ({ page }) => {
    await open(page, wizard(), "/drivers/new");

    await next(page);

    await expect(page.getByText("La photo de profil est obligatoire.")).toBeVisible();
    await expect(page.getByText("Étape 1 sur 4")).toBeVisible();
  });

  test("shows the server's reason when the e-mail is already used", async ({ page }) => {
    const api = wizard().on("POST /drivers", failure(422, "DRIVER_EMAIL_TAKEN"));
    await open(page, api, "/drivers/new");
    await fillPersonal(page);
    await next(page);
    await field(page, "emergency.lastName").fill("Nang Ndong");
    await field(page, "emergency.firstName").fill("Brigitte");
    await field(page, "emergency.phone").fill("66685807");
    await field(page, "emergency.relationship").selectOption("MOTHER");
    await field(page, "emergency.address").fill("Akanda");
    await next(page);
    await field(page, "vehicleId").selectOption("vehicle-1");
    await field(page, "training.trainingCenterId").selectOption("center-1");
    await field(page, "training.trainingDate").fill("2026-10-15");
    await next(page);
    await page.getByRole("button", { name: "Continuer" }).click();
    await page.getByRole("dialog").getByLabel("Mot de passe", { exact: true }).fill("Str0ng!Pass");
    await page.getByRole("dialog").getByLabel("Confirmer le mot de passe").fill("Str0ng!Pass");
    await page.getByRole("button", { name: "Créer le chauffeur" }).click();

    await expect(page.getByText("Un chauffeur avec cet email existe déjà.")).toBeVisible();
  });
});
