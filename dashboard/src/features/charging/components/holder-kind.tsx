import { Badge } from "@/components/ui/badge";
import type { ChargingSessionRow, HolderKind } from "@/features/charging/types";

// A session belongs to a NDZela driver (an idTag linked to one of them, with the car they rent) or to the public.
export function holderKindOf(session: Pick<ChargingSessionRow, "holder">): HolderKind {
  return session.holder ? "DRIVER" : "PUBLIC";
}

export function HolderKindBadge({ kind }: { kind: HolderKind }) {
  return kind === "DRIVER" ? <Badge variant="info">Chauffeur NDZela</Badge> : <Badge>Public</Badge>;
}
