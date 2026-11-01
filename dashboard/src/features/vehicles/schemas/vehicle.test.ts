import { describe, expect, it } from "vitest";
import { vehicleSchema } from "@/features/vehicles/schemas/vehicle";

const valid = {
  fleetNumber: "V-29", brand: "Dongfeng", model: "Box", year: "2025", plate: "LZ-101-AA",
  color: "Bleu", vin: "", purchasePrice: "13000000", dailyRent: "23000",
};

describe("vehicleSchema", () => {
  it("accepts a complete vehicle, with an empty VIN", () => {
    expect(vehicleSchema.safeParse(valid).success).toBe(true);
  });

  it("refuses an amount that is not whole digits or is zero", () => {
    expect(vehicleSchema.safeParse({ ...valid, dailyRent: "23 000" }).success).toBe(false);
    expect(vehicleSchema.safeParse({ ...valid, dailyRent: "0" }).success).toBe(false);
    expect(vehicleSchema.safeParse({ ...valid, purchasePrice: "-5" }).success).toBe(false);
  });

  it("keeps the year between 1990 and next year", () => {
    const next = String(new Date().getFullYear() + 1);
    const after = String(new Date().getFullYear() + 2);
    expect(vehicleSchema.safeParse({ ...valid, year: next }).success).toBe(true);
    expect(vehicleSchema.safeParse({ ...valid, year: after }).success).toBe(false);
    expect(vehicleSchema.safeParse({ ...valid, year: "1989" }).success).toBe(false);
    expect(vehicleSchema.safeParse({ ...valid, year: "25" }).success).toBe(false);
  });

  it("trims and requires the text fields", () => {
    expect(vehicleSchema.safeParse({ ...valid, plate: "   " }).success).toBe(false);
    expect(vehicleSchema.parse({ ...valid, brand: "  Dongfeng " }).brand).toBe("Dongfeng");
  });
});
