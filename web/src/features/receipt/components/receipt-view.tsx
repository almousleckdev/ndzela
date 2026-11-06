import { CircleCheck, CircleX } from "lucide-react";
import { Container } from "@/components/ui/container";
import { dateTime, dayLabel, money } from "@/features/receipt/format";
import type { PublicReceipt } from "@/features/receipt/types";

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-4 border-b border-line py-3 text-sm last:border-0">
      <dt className="text-muted">{label}</dt>
      <dd className="text-right font-medium">{value}</dd>
    </div>
  );
}

// The payment a scanned receipt stands for
export function ReceiptView({ receipt: r }: { receipt: PublicReceipt }) {
  const Icon = r.cancelled ? CircleX : CircleCheck;
  return (
    <section className="py-12 sm:py-16">
      <Container className="max-w-xl">
        <div className={`flex items-center gap-3 rounded-2xl p-5 ${r.cancelled ? "bg-red-50 text-red-700" : "bg-emerald-50 text-emerald-700"}`}>
          <Icon className="size-8 shrink-0" aria-hidden />
          <div>
            <h1 className="font-display text-2xl font-bold">{r.cancelled ? "Paiement annulé" : "Paiement confirmé"}</h1>
            <p className="text-sm">{money(r.total, r.currency)} · {dateTime(r.paidAt)}</p>
          </div>
        </div>
        <dl className="mt-6 rounded-2xl border border-line px-5">
          <Row label="Référence" value={r.reference} />
          <Row label="Chauffeur" value={r.payerName} />
          <Row label="Contrat" value={r.contractNumber} />
          <Row label="Opérateur" value={r.operator} />
          {r.maskedPhone && <Row label="Numéro" value={r.maskedPhone} />}
          <Row label="Loyer payé" value={money(r.amount, r.currency)} />
          {r.fee > 0 && <Row label="Frais opérateur" value={money(r.fee, r.currency)} />}
          <Row label="Total" value={money(r.total, r.currency)} />
          <Row label="Bénéficiaire" value={r.beneficiaryName} />
        </dl>
        {r.days.length > 0 && (
          <>
            <h2 className="mt-8 font-display text-xl font-bold">Jours réglés</h2>
            <ul className="mt-3 divide-y divide-line rounded-2xl border border-line">
              {r.days.map((d) => (
                <li key={`${d.day}-${d.kind}`} className="flex items-center justify-between gap-4 px-5 py-3 text-sm">
                  <span className="capitalize">{dayLabel(d.day)}{d.kind === "PENALTY" ? " (pénalité)" : ""}</span>
                  <span className="text-right">
                    <span className="font-medium">{money(d.amount, r.currency)}</span>
                    <span className={`block text-xs ${d.onTime ? "text-emerald-700" : "text-action"}`}>{d.onTime ? "à temps" : "en retard"}</span>
                  </span>
                </li>
              ))}
            </ul>
          </>
        )}
        {r.creditKept > 0 && <p className="mt-4 text-sm text-muted">Crédit conservé : {money(r.creditKept, r.currency)}</p>}
      </Container>
    </section>
  );
}
