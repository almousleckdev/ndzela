import { describe, expect, it } from "vitest";
import { formatFcfa } from "@/lib/money";

describe("formatFcfa", () => {
  it("groups thousands the French way with a no-break space", () => {
    expect(formatFcfa(23000)).toBe("23 000 FCFA");
    expect(formatFcfa(28704000)).toBe("28 704 000 FCFA");
  });

  it("never uses the narrow no-break space that fonts draw too tight", () => {
    expect(formatFcfa(1234567)).not.toContain(" ");
  });

  it("writes zero and small amounts plainly", () => {
    expect(formatFcfa(0)).toBe("0 FCFA");
    expect(formatFcfa(999)).toBe("999 FCFA");
  });
});
