import { describe, expect, it } from "vitest";
import { describeWindow, timeText, WEEKDAY_OPTIONS } from "@/features/free-charging/lib/free-charging-window";

describe("the free window in words", () => {
  it("writes a time as hours and minutes", () => {
    expect(timeText("00:00")).toBe("00h00");
    expect(timeText("23:59")).toBe("23h59");
    expect(timeText("08:05")).toBe("08h05");
  });

  it("describes the whole-day default", () => {
    expect(describeWindow({ day: "SUNDAY", from: "00:00", until: "23:59", endOfDay: true })).toBe("le dimanche, de 00h00 à 23h59 (heure du Gabon)");
  });

  it("describes a bounded window on another day", () => {
    expect(describeWindow({ day: "SATURDAY", from: "10:30", until: "20:00", endOfDay: false })).toBe("le samedi, de 10h30 à 20h00 (heure du Gabon)");
  });

  it("offers the seven weekdays in order from Monday", () => {
    expect(WEEKDAY_OPTIONS.map((option) => option.value)).toEqual(["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"]);
    expect(WEEKDAY_OPTIONS.every((option) => option.label.length > 0)).toBe(true);
  });
});
