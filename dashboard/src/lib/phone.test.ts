import { describe, expect, it } from "vitest";
import { joinPhone, nationalNumberSchema, splitPhone } from "@/lib/phone";

describe("splitPhone", () => {
  it("splits a stored number into its code and national part", () => {
    expect(splitPhone("+241 62821624", ["+241", "+223"])).toEqual({ code: "+241", number: "62821624" });
  });

  it("prefers the longest matching code", () => {
    expect(splitPhone("+2410 123", ["+241", "+2410"])).toEqual({ code: "+2410", number: "123" });
  });

  it("returns a number without a known code whole", () => {
    expect(splitPhone("062821624", ["+241"])).toEqual({ code: null, number: "062821624" });
    expect(splitPhone(null, ["+241"])).toEqual({ code: null, number: "" });
  });
});

describe("joinPhone", () => {
  it("keeps the digits only and prefixes the code", () => {
    expect(joinPhone("+241", "62 82 18 55")).toBe("+241 62821855");
  });

  it("leaves an empty number empty", () => {
    expect(joinPhone("+241", "  ")).toBe("");
  });
});

describe("nationalNumberSchema", () => {
  it.each(["", "62821855", "62 82 18 55", "62.82.18.55"])("accepts %j", (value) => {
    expect(nationalNumberSchema.safeParse(value).success).toBe(true);
  });

  it.each(["12345", "1234567890123", "abc12345"])("refuses %j", (value) => {
    expect(nationalNumberSchema.safeParse(value).success).toBe(false);
  });
});
