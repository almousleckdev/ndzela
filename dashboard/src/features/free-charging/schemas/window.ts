import { z } from "zod";

const WEEKDAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"] as const;

export const windowSchema = z
  .object({
    day: z.enum(WEEKDAYS, { message: "Choisissez un jour" }),
    from: z.string().min(1, "Heure de début obligatoire"),
    until: z.string(),
    endOfDay: z.boolean(),
  })
  .superRefine((values, context) => {
    if (values.endOfDay) return;
    if (!values.until) {
      context.addIssue({ code: "custom", path: ["until"], message: "Heure de fin obligatoire" });
    } else if (values.from && values.until <= values.from) {
      context.addIssue({ code: "custom", path: ["until"], message: "La fin doit venir après le début" });
    }
  });

export type WindowValues = z.infer<typeof windowSchema>;
