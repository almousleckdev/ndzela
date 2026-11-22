import type { FreeWindow, Weekday } from "@/features/free-charging/types";

export const WEEKDAY_OPTIONS: { value: Weekday; label: string; sentence: string }[] = [
  { value: "MONDAY", label: "Lundi", sentence: "lundi" },
  { value: "TUESDAY", label: "Mardi", sentence: "mardi" },
  { value: "WEDNESDAY", label: "Mercredi", sentence: "mercredi" },
  { value: "THURSDAY", label: "Jeudi", sentence: "jeudi" },
  { value: "FRIDAY", label: "Vendredi", sentence: "vendredi" },
  { value: "SATURDAY", label: "Samedi", sentence: "samedi" },
  { value: "SUNDAY", label: "Dimanche", sentence: "dimanche" },
];

export function timeText(time: string): string {
  const [hours, minutes] = time.split(":");
  return `${hours}h${minutes}`;
}

// The window as a sentence: "le dimanche, de 00h00 à 23h59 (heure du Gabon)".
export function describeWindow(window: FreeWindow): string {
  const day = WEEKDAY_OPTIONS.find((option) => option.value === window.day)?.sentence ?? window.day.toLowerCase();
  return `le ${day}, de ${timeText(window.from)} à ${timeText(window.until)} (heure du Gabon)`;
}
