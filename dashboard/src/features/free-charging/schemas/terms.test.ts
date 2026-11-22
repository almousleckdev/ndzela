import { describe, expect, it } from "vitest";
import { termsSchema } from "@/features/free-charging/schemas/terms";

const valid = { startsOn: "2026-11-01", endsOn: "2026-12-01", conditions: "" };
const messages = (values: object) => {
  const result = termsSchema.safeParse(values);
  return result.success ? [] : result.error.issues.map((issue) => issue.message);
};

describe("termsSchema", () => {
  it("accepts complete terms and terms without an end date", () => {
    expect(messages(valid)).toEqual([]);
    expect(messages({ ...valid, endsOn: "" })).toEqual([]);
  });

  it("requires a start date", () => {
    expect(messages({ ...valid, startsOn: "" })).toContain("Date de début obligatoire");
  });

  it("refuses an end before the start", () => {
    expect(messages({ ...valid, endsOn: "2026-10-31" })).toContain("La date de fin ne peut pas précéder la date de début");
  });

  it("limits the conditions to 1000 characters", () => {
    expect(messages({ ...valid, conditions: "a".repeat(1001) })).toContain("1000 caractères maximum");
    expect(messages({ ...valid, conditions: "a".repeat(1000) })).toEqual([]);
  });
});
