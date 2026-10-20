import { expect, test } from "@playwright/test";
import { failure, MockApi } from "./support/mock-api";
import { page0, profile } from "./support/fixtures";

test.describe("signing in", () => {
  test("password, e-mail code and authenticator code lead to the dashboard", async ({ page }) => {
    const api = new MockApi()
      .on("POST /auth/refresh", failure(401, "SESSION_EXPIRED"))
      .on("POST /auth/login", { challengeToken: "c1", nextStep: "EMAIL_OTP", resendInSeconds: 60 })
      .on("POST /auth/verify-otp", { challengeToken: "c1", nextStep: "MFA_VERIFY", resendInSeconds: 0 })
      .on("POST /auth/verify-totp", { accessToken: "token", tokenType: "Bearer", expiresInSeconds: 900 })
      .on("GET /profile/me", profile(["drivers:read"]))
      .on("GET /drivers", page0());
    await api.attach(page);
    await page.goto("/login");

    await page.getByLabel("Adresse Email").fill("manager@ndzela.test");
    await page.getByRole("textbox", { name: "Mot de passe" }).fill("Str0ng!Pass");
    await page.getByRole("button", { name: "Connexion Sécurisée" }).click();

    await page.getByLabel("Code à 6 chiffres").fill("123456");
    await page.getByRole("button", { name: "Vérifier le code" }).click();
    await page.getByLabel("Code à 6 chiffres").fill("654321");
    await page.getByRole("button", { name: "Terminer la connexion" }).click();

    await expect(page).toHaveURL("/");
    expect(api.requestsTo("POST /auth/login")[0].body).toEqual({ email: "manager@ndzela.test", password: "Str0ng!Pass" });
    expect(api.requestsTo("POST /auth/verify-otp")[0].body).toEqual({ challengeToken: "c1", otp: "123456" });
    expect(api.requestsTo("POST /auth/verify-totp")[0].body).toEqual({ challengeToken: "c1", code: "654321" });
  });

  test("a wrong password shows an error and stays on the first step", async ({ page }) => {
    const api = new MockApi()
      .on("POST /auth/refresh", failure(401, "SESSION_EXPIRED"))
      .on("POST /auth/login", failure(401, "INVALID_CREDENTIALS"));
    await api.attach(page);
    await page.goto("/login");

    await page.getByLabel("Adresse Email").fill("manager@ndzela.test");
    await page.getByRole("textbox", { name: "Mot de passe" }).fill("Wrong!Pass1");
    await page.getByRole("button", { name: "Connexion Sécurisée" }).click();

    await expect(page.getByText("Email ou mot de passe incorrect.")).toBeVisible();
    await expect(page.getByRole("textbox", { name: "Mot de passe" })).toBeVisible();
  });

  test("a visitor without a session is sent to the login page", async ({ page }) => {
    const api = new MockApi().on("POST /auth/refresh", failure(401, "SESSION_EXPIRED"));
    await api.attach(page);
    await page.goto("/drivers");
    await expect(page).toHaveURL(/\/login$/);
  });
});
