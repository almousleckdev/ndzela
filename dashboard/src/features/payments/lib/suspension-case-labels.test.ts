import { describe, expect, it } from "vitest";
import { helperLabel, reasonLabel, scoreLabel } from "@/features/payments/lib/suspension-case-labels";

describe("suspension case labels", () => {
  it("names the known reasons and leaves an unknown code readable", () => {
    expect(reasonLabel("OVERLAP")).toBe("Deux véhicules chargés en même temps");
    expect(reasonLabel("BATTERY_CHAIN")).toBe("Batterie incohérente entre deux recharges");
    expect(reasonLabel("ENERGY")).toBe("Énergie très supérieure à l'habitude");
    expect(reasonLabel("SOMETHING_NEW")).toBe("SOMETHING_NEW");
  });

  it("shows a driver of ours in full and any other account only by its short reference", () => {
    expect(helperLabel({ helper: { id: "d1", name: "Awa Mba", vehicle: "V-002 · GA-123" }, helperReference: "abcdef0123456789" })).toEqual({ primary: "Awa Mba", secondary: "V-002 · GA-123" });
    expect(helperLabel({ helper: null, helperReference: "abcdef0123456789" })).toEqual({ primary: "Compte Zella", secondary: "réf. abcdef01" });
  });

  it("lists the reasons of a flagged case, and says plainly when nothing is wrong or nothing is known", () => {
    expect(scoreLabel({ score: 100, flagged: true, reasons: ["OVERLAP", "ENERGY"] })).toBe("Deux véhicules chargés en même temps · Énergie très supérieure à l'habitude");
    expect(scoreLabel({ score: 0, flagged: false, reasons: [] })).toBe("Rien d'anormal");
    expect(scoreLabel({ score: null, flagged: false, reasons: [] })).toBe("Non évalué");
  });
});
