import { describe, expect, it } from "vitest";
import { generateTemporaryPassword } from "@/features/drivers/lib/temporary-password";

describe("generateTemporaryPassword", () => {
  it("makes 12 characters with a lower-case letter, an upper-case letter and a digit", () => {
    for (let i = 0; i < 50; i += 1) {
      const password = generateTemporaryPassword();
      expect(password).toHaveLength(12);
      expect(password).toMatch(/[a-z]/);
      expect(password).toMatch(/[A-Z]/);
      expect(password).toMatch(/\d/);
    }
  });

  it("never uses characters that are easy to confuse when read out loud", () => {
    for (let i = 0; i < 50; i += 1) {
      expect(generateTemporaryPassword()).not.toMatch(/[0O1lI]/);
    }
  });

  it("gives a different password each time", () => {
    expect(new Set(Array.from({ length: 20 }, () => generateTemporaryPassword())).size).toBe(20);
  });
});
