import { siteConfig } from "@/config/site";

// Whether the office is open at `now`, read in the company's own timezone.
export function isOpenAt(now: Date): boolean {
  const parts = new Intl.DateTimeFormat("en-GB", { timeZone: siteConfig.timezone, weekday: "short", hour: "2-digit", minute: "2-digit", hourCycle: "h23" }).formatToParts(now);
  const get = (type: string) => parts.find((part) => part.type === type)?.value ?? "";
  const day = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"].indexOf(get("weekday"));
  const minutes = Number(get("hour")) * 60 + Number(get("minute"));
  const today = siteConfig.hours.find((entry) => (entry.days as readonly number[]).includes(day));
  return Boolean(today?.hours && minutes >= today.hours.from && minutes < today.hours.to);
}
