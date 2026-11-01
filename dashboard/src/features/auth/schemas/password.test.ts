import { describe, expect, it } from "vitest";
import { passwordSchema } from "@/features/auth/schemas/password";
import { changePasswordSchema, credentialsSchema, otpSchema, resetPasswordSchema } from "@/features/auth/schemas/auth";

const messages = (result: { success: boolean; error?: { issues: { message: string }[] } }) => result.error?.issues.map((i) => i.message) ?? [];

describe("passwordSchema (mirrors the backend StrongPassword rule)", () => {
  it("accepts a strong password", () => {
    expect(passwordSchema.safeParse("Str0ng!Pass").success).toBe(true);
  });

  it.each([
    ["Sh0rt!a", "Au moins 8 caractères"],
    ["lowercase1!", "Au moins une majuscule"],
    ["UPPERCASE1!", "Au moins une minuscule"],
    ["NoDigits!!", "Au moins un chiffre"],
    ["NoSpecial12", "Au moins un caractère spécial"],
  ])("refuses %s", (password, message) => {
    expect(messages(passwordSchema.safeParse(password))).toContain(message);
  });
});

describe("auth forms", () => {
  it("needs a valid email and a password to sign in", () => {
    expect(credentialsSchema.safeParse({ email: "a@b.co", password: "x" }).success).toBe(true);
    expect(credentialsSchema.safeParse({ email: "nope", password: "x" }).success).toBe(false);
    expect(credentialsSchema.safeParse({ email: "a@b.co", password: "" }).success).toBe(false);
  });

  it("asks for exactly six digits as a code", () => {
    expect(otpSchema.safeParse({ code: "123456" }).success).toBe(true);
    expect(otpSchema.safeParse({ code: "12345" }).success).toBe(false);
    expect(otpSchema.safeParse({ code: "12345a" }).success).toBe(false);
  });

  it("points at the confirmation field when passwords differ", () => {
    const result = resetPasswordSchema.safeParse({ newPassword: "Str0ng!Pass", confirmPassword: "Different1!" });
    expect(result.success).toBe(false);
    expect(result.error?.issues[0].path).toEqual(["confirmPassword"]);
  });

  it("needs the current password to change it", () => {
    const base = { newPassword: "Str0ng!Pass", confirmPassword: "Str0ng!Pass" };
    expect(changePasswordSchema.safeParse({ ...base, currentPassword: "" }).success).toBe(false);
    expect(changePasswordSchema.safeParse({ ...base, currentPassword: "Old1!pass" }).success).toBe(true);
  });
});
