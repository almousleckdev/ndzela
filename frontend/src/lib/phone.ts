import { z } from "zod";

/** The national part of a phone number: 6 to 12 digits, spaces and dashes allowed. */
export const nationalNumberSchema = z
  .string()
  .max(30, "Le numéro de téléphone est trop long")
  .refine((value) => value === "" || /^\d{6,12}$/.test(value.replace(/[\s.-]/g, "")), "Numéro invalide (6 à 12 chiffres)");

/** Splits "+241 62821624" into its code and number; a number without a known code is returned whole. */
export function splitPhone(value: string | null, codes: string[]): { code: string | null; number: string } {
  const text = (value ?? "").trim();
  const code = [...codes].sort((a, b) => b.length - a.length).find((candidate) => candidate && text.startsWith(candidate));
  return code ? { code, number: text.slice(code.length).trim() } : { code: null, number: text };
}

/** "+241" and "62 82 18 55" become "+241 62821855"; an empty number stays empty. */
export function joinPhone(code: string, number: string): string {
  const digits = number.replace(/\D/g, "");
  return digits ? `${code} ${digits}`.trim() : "";
}
