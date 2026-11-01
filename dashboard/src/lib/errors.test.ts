import { describe, expect, it } from "vitest";
import { ApiException } from "@/lib/api/api-error";
import { getErrorMessage, isSessionExpired } from "@/lib/errors";

const apiError = (code: string) =>
  new ApiException({ timestamp: "", status: 422, code, message: "english", path: "/x" });

describe("getErrorMessage", () => {
  it("translates a known API code into French", () => {
    expect(getErrorMessage(apiError("DRIVER_TOO_YOUNG"), "fallback")).toBe("Le chauffeur doit avoir au moins 18 ans.");
  });

  it("uses the caller's fallback for an unknown API code", () => {
    expect(getErrorMessage(apiError("SOMETHING_NEW"), "Impossible d'enregistrer.")).toBe("Impossible d'enregistrer.");
  });

  it("hides any other thrown value behind a generic message", () => {
    expect(getErrorMessage(new Error("boom"), "fallback")).toBe("Une erreur inattendue s'est produite.");
  });
});

describe("isSessionExpired", () => {
  it("is true only for the SESSION_EXPIRED API code", () => {
    expect(isSessionExpired(apiError("SESSION_EXPIRED"))).toBe(true);
    expect(isSessionExpired(apiError("ACCESS_DENIED"))).toBe(false);
    expect(isSessionExpired(new Error("x"))).toBe(false);
  });
});
