export const VEHICLE_COLORS = [
  { name: "Blanc", hex: "#FFFFFF" },
  { name: "Noir", hex: "#111827" },
  { name: "Gris", hex: "#9CA3AF" },
  { name: "Argent", hex: "#D1D5DB" },
  { name: "Rouge", hex: "#DC2626" },
  { name: "Bleu", hex: "#2563EB" },
  { name: "Vert", hex: "#16A34A" },
  { name: "Jaune", hex: "#FACC15" },
  { name: "Orange", hex: "#EA580C" },
  { name: "Marron", hex: "#78350F" },
] as const;

const HEX = /^#[0-9a-fA-F]{6}$/;

export const isHexColor = (value: string) => HEX.test(value);

/** The French name of a known colour, otherwise the stored text itself. */
export function colorName(value: string): string {
  return VEHICLE_COLORS.find((color) => color.hex.toLowerCase() === value.toLowerCase())?.name ?? value;
}
