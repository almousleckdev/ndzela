import { describe, expect, it } from "vitest";
import { reasonLabel } from "@/features/charging/lib/stop-reason";

describe("reasonLabel", () => {
  it("labels the end of the free window", () => {
    expect(reasonLabel("FREE_WINDOW_ENDED")).toBe("Fin du créneau gratuit (22h)");
  });

  it("keeps the existing labels and fallbacks", () => {
    expect(reasonLabel("PowerLoss")).toBe("Coupure d'électricité");
    expect(reasonLabel(null, false)).toBeNull();
    expect(reasonLabel(null)).toBe("Montant atteint");
  });
});
