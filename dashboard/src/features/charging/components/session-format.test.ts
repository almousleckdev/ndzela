import { describe, expect, it } from "vitest";
import { reasonLabel } from "@/features/charging/components/session-format";

describe("reasonLabel", () => {
  it("names three reasons only: the driver, the amount, the power", () => {
    expect(reasonLabel("Remote")).toBe("Arrêt par le conducteur");
    expect(reasonLabel("Local")).toBe("Arrêt par le conducteur");
    expect(reasonLabel("EVDisconnected")).toBe("Arrêt par le conducteur");
    expect(reasonLabel("Other")).toBe("Montant atteint");
    expect(reasonLabel("PowerLoss")).toBe("Coupure d'électricité");
  });

  it("never leaves a finished session without a reason", () => {
    expect(reasonLabel(null)).toBe("Montant atteint");
    expect(reasonLabel("SomethingNew")).toBe("Montant atteint");
  });

  it("shows nothing for a session still running", () => {
    expect(reasonLabel(null, false)).toBeNull();
  });
});
