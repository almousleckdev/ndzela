import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { timeAgo } from "@/lib/time";

describe("timeAgo", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-10-14T12:00:00Z"));
  });
  afterEach(() => vi.useRealTimers());

  it("says now for the last seconds", () => {
    expect(timeAgo("2026-10-14T11:59:40Z")).toBe("à l'instant");
  });

  it("counts minutes, hours and days in French", () => {
    expect(timeAgo("2026-10-14T11:55:00Z")).toBe("il y a 5 minutes");
    expect(timeAgo("2026-10-14T09:00:00Z")).toBe("il y a 3 heures");
    expect(timeAgo("2026-10-13T12:00:00Z")).toBe("hier");
  });
});
