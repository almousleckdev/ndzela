import { describe, expect, it } from "vitest";
import { eligibilityLabel, formatDay, STATUS_LABELS } from "@/features/free-charging/lib/free-charging-labels";

describe("free charging labels", () => {
  it("names the window without hard-coding its times", () => {
    expect(eligibilityLabel("OUTSIDE_WINDOW")).toBe("Hors créneau");
  });

  it("has a French label for every status", () => {
    expect(Object.values(STATUS_LABELS)).toEqual(["Actif", "Suspendu", "Révoqué"]);
  });

  it("writes a day as day/month/year", () => {
    expect(formatDay("2026-11-22")).toBe("22/11/2026");
    expect(formatDay(null)).toBe("");
  });
});
