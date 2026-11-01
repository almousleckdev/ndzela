import { defineConfig, devices } from "@playwright/test";

const PORT = 3100;
export const API_URL = "http://localhost:8080/api/v1";

// The flows run against a production build with the API mocked in the browser (see e2e/support/mock-api.ts): no backend or database is needed.
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [["list"], ["html", { open: "never" }]] : "list",
  use: { baseURL: `http://localhost:${PORT}`, trace: "retain-on-failure" },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
  webServer: {
    command: `npm run build && npx next start -p ${PORT}`,
    url: `http://localhost:${PORT}/login`,
    env: { NEXT_PUBLIC_API_URL: API_URL },
    reuseExistingServer: !process.env.CI,
    timeout: 240_000,
  },
});
