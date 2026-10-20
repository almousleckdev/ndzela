import { describe, expect, it } from "vitest";
import { NO_RANGE, describeRange, rangeOf } from "@/lib/date-range";

// Wednesday 14 October 2026, local time
const today = new Date(2026, 9, 14, 12, 0, 0);

describe("rangeOf", () => {
  it("counts presets from the given day, inclusive", () => {
    expect(rangeOf("today", today)).toMatchObject({ from: "2026-10-14", to: "2026-10-14" });
    expect(rangeOf("yesterday", today)).toMatchObject({ from: "2026-10-13", to: "2026-10-13" });
    expect(rangeOf("last7", today)).toMatchObject({ from: "2026-10-08", to: "2026-10-14" });
    expect(rangeOf("last30", today)).toMatchObject({ from: "2026-09-15", to: "2026-10-14" });
  });

  it("knows the month and year boundaries", () => {
    expect(rangeOf("thisMonth", today)).toMatchObject({ from: "2026-10-01", to: "2026-10-14" });
    expect(rangeOf("lastMonth", today)).toMatchObject({ from: "2026-09-01", to: "2026-09-30" });
    expect(rangeOf("thisYear", today)).toMatchObject({ from: "2026-01-01", to: "2026-10-14" });
  });

  it("leaves both sides open for the whole period", () => {
    expect(rangeOf("all", today)).toBe(NO_RANGE);
  });
});

describe("describeRange", () => {
  it("uses the preset's own label", () => {
    expect(describeRange(rangeOf("last7", today))).toBe("7 derniers jours");
  });

  it("describes custom ranges, one side or both", () => {
    expect(describeRange({ from: "2026-10-01", to: "2026-10-01", preset: "custom" })).toMatch(/^1 oct 2026$/i);
    expect(describeRange({ from: "", to: "2026-10-05", preset: "custom" })).toMatch(/^Jusqu'au 5 oct/i);
    expect(describeRange({ from: "2026-10-05", to: "", preset: "custom" })).toMatch(/^Depuis le 5 oct/i);
    expect(describeRange({ from: "", to: "", preset: "custom" })).toBe("Toute la période");
  });
});
