import { describe, expect, it } from "vitest";
import { NAV_SECTIONS } from "@/components/layout/nav-items";
import { PERMISSIONS } from "@/features/access/lib/permissions";

describe("the sidebar entry for the earnings", () => {
  it("is under Finance and needs earnings:read", () => {
    const items = NAV_SECTIONS.flatMap((section) => section.items);
    const entry = items.find((item) => item.href === "/finance/earnings");

    expect(entry?.label).toBe("Total encaissé");
    expect(entry?.permission).toBe(PERMISSIONS.EARNINGS_READ);
  });
});
