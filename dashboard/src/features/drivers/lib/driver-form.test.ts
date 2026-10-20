import { describe, expect, it } from "vitest";
import { EMPTY_DRIVER, toRequestBody, valuesFromDriver } from "@/features/drivers/lib/driver-form";
import { storedDriver, validDriverValues } from "@/test/driver-fixtures";

const codes = { primary: "+241", secondary: "+241", emergency: "+241" };

describe("toRequestBody", () => {
  it("joins each dialling code with its number and sends no secondary phone when empty", () => {
    const body = toRequestBody(validDriverValues, codes);
    expect(body.personal.phonePrimary).toBe("+241 62143500");
    expect(body.personal.phoneSecondary).toBeNull();
    expect(body.emergency.phone).toBe("+241 66685807");
  });

  it("turns a blank certificate number into null", () => {
    expect(toRequestBody(validDriverValues, codes).training.certificateNumber).toBeNull();
    const withCertificate = { ...validDriverValues, training: { ...validDriverValues.training, certificateNumber: " C-12 " } };
    expect(toRequestBody(withCertificate, codes).training.certificateNumber).toBe("C-12");
  });

  it("adds the vehicle, the deposit as a number and the password only when creating", () => {
    expect(toRequestBody(validDriverValues, codes)).not.toHaveProperty("password");
    const created = toRequestBody(validDriverValues, codes, { vehicleId: "vehicle-1", password: "Str0ng!Pass" });
    expect(created).toMatchObject({ vehicleId: "vehicle-1", password: "Str0ng!Pass", contract: { deposit: 500000 } });
  });
});

describe("valuesFromDriver", () => {
  it("splits stored phone numbers back into code and number", () => {
    const { values, codes: split } = valuesFromDriver(storedDriver, ["+241", "+223"]);
    expect(values.personal.phonePrimary).toBe("62143500");
    expect(values.personal.phoneSecondary).toBe("76290216");
    expect(values.emergency.phone).toBe("66685807");
    expect(split).toEqual({ primary: "+241", secondary: "+241", emergency: "+241" });
  });

  it("carries the training centre, the deposit and leaves the vehicle choice empty", () => {
    const { values } = valuesFromDriver(storedDriver, ["+241"]);
    expect(values.training).toEqual({ trainingCenterId: "center-1", trainingDate: "2026-10-15", certificateNumber: "" });
    expect(values.contract.deposit).toBe("500000");
    expect(values.vehicleId).toBe("");
  });

  it("round-trips through the request body", () => {
    const { values, codes: split } = valuesFromDriver(storedDriver, ["+241"]);
    const body = toRequestBody(values, { primary: split.primary!, secondary: split.secondary!, emergency: split.emergency! });
    expect(body.personal.phonePrimary).toBe(storedDriver.personal.phonePrimary.replace(/\s+/g, " "));
  });
});

describe("EMPTY_DRIVER", () => {
  it("starts with the default deposit", () => {
    expect(EMPTY_DRIVER.contract.deposit).toBe("250000");
  });
});
