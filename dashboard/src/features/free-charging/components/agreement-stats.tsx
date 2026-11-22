import { KpiBand } from "@/components/layout/page";
import type { Agreement } from "@/features/free-charging/types";

export function AgreementStats({ items }: { items: Agreement[] }) {
  const active = items.filter((agreement) => agreement.status !== "REVOKED");
  return (
    <KpiBand items={[
      { label: "Dans le programme", value: active.length },
      { label: "Éligibles maintenant", value: active.filter((agreement) => agreement.eligibility === "ELIGIBLE").length },
      { label: "À régulariser", value: active.filter((agreement) => agreement.eligibility === "UNPAID_DAYS").length },
    ]} />
  );
}
