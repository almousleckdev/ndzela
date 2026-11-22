import { describe, expect, it } from "vitest";
import { windowSchema } from "@/features/free-charging/schemas/window";

const ok = { day: "SUNDAY", from: "00:00", until: "", endOfDay: true };

describe("the window form", () => {
  it("accepts the whole day without an end time", () => {
    expect(windowSchema.safeParse(ok).success).toBe(true);
  });

  it("accepts a bounded window", () => {
    expect(windowSchema.safeParse({ day: "SATURDAY", from: "09:00", until: "17:30", endOfDay: false }).success).toBe(true);
  });

  it("asks for an end time when the window does not run to the end of the day", () => {
    const result = windowSchema.safeParse({ ...ok, endOfDay: false });
    expect(result.success).toBe(false);
    expect(result.error?.issues[0].path).toEqual(["until"]);
  });

  it("refuses an end that is not after the start", () => {
    expect(windowSchema.safeParse({ day: "SUNDAY", from: "12:00", until: "12:00", endOfDay: false }).success).toBe(false);
    expect(windowSchema.safeParse({ day: "SUNDAY", from: "22:00", until: "12:00", endOfDay: false }).success).toBe(false);
  });

  it("requires a start time", () => {
    expect(windowSchema.safeParse({ ...ok, from: "" }).success).toBe(false);
  });

  it("ignores the end time when the window runs to the end of the day", () => {
    expect(windowSchema.safeParse({ ...ok, from: "23:00", until: "01:00" }).success).toBe(true);
  });
});
