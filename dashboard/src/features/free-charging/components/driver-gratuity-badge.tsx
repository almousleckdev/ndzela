import { Badge } from "@/components/ui/badge";
import { canAdd, isChecked, type Selection } from "@/features/free-charging/lib/selection";
import type { DriverOption } from "@/features/free-charging/types";

export function DriverGratuityBadge({ driver, selection }: { driver: DriverOption; selection: Selection }) {
  const checked = isChecked(driver, selection);
  if (driver.inProgram) {
    if (!checked) return <Badge variant="danger">Sera retiré</Badge>;
    return driver.agreementStatus === "SUSPENDED" ? <Badge variant="warning">Gratuité suspendue</Badge> : <Badge variant="success">Dans le programme</Badge>;
  }
  if (checked) return <Badge variant="info">Sera ajouté</Badge>;
  if (!driver.linked) return <Badge variant="warning">Compte Zella non lié</Badge>;
  return <Badge>{canAdd(driver) ? "Hors programme" : "Chauffeur non actif"}</Badge>;
}
