import { describe, expect, it } from "vitest";
import { driverSchema } from "@/features/drivers/schemas/driver";
import { validDriverValues } from "@/test/driver-fixtures";

const withPersonal = (patch: Record<string, string>) => ({ ...validDriverValues, personal: { ...validDriverValues.personal, ...patch } });
const failures = (values: unknown) => {
  const result = driverSchema.safeParse(values);
  return result.success ? [] : result.error.issues.map((issue) => issue.path.join("."));
};

describe("driverSchema", () => {
  it("accepts a complete driver", () => {
    expect(driverSchema.safeParse(validDriverValues).success).toBe(true);
  });

  it("refuses a driver under 18", () => {
    const year = new Date().getFullYear() - 17;
    expect(failures(withPersonal({ birthDate: `${year}-01-01` }))).toContain("personal.birthDate");
  });

  it("refuses issue dates in the future", () => {
    expect(failures(withPersonal({ idIssueDate: "2999-01-01" }))).toContain("personal.idIssueDate");
    expect(failures(withPersonal({ licenceIssueDate: "2999-01-01" }))).toContain("personal.licenceIssueDate");
  });

  it("needs a primary phone but not a secondary one", () => {
    expect(failures(withPersonal({ phonePrimary: "" }))).toContain("personal.phonePrimary");
    expect(failures(withPersonal({ phoneSecondary: "" }))).toEqual([]);
    expect(failures(withPersonal({ phoneSecondary: "12" }))).toContain("personal.phoneSecondary");
  });

  it("needs a valid e-mail, a known licence category and a deposit above zero", () => {
    expect(failures(withPersonal({ email: "not-an-email" }))).toContain("personal.email");
    expect(failures(withPersonal({ licenceCategory: "Z" }))).toContain("personal.licenceCategory");
    expect(failures({ ...validDriverValues, contract: { deposit: "0" } })).toContain("contract.deposit");
    expect(failures({ ...validDriverValues, contract: { deposit: "250 000" } })).toContain("contract.deposit");
  });

  it("needs a training centre", () => {
    expect(failures({ ...validDriverValues, training: { ...validDriverValues.training, trainingCenterId: "" } })).toContain("training.trainingCenterId");
  });
});
