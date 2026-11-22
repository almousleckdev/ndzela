import type { ChangeResult } from "@/features/free-charging/hooks/use-apply-changes";

export function BulkOutcome({ result }: { result: ChangeResult }) {
  return (
    <div role="status" className="space-y-3">
      {result.added > 0 && <p className="text-sm font-medium text-ink">{result.added === 1 ? "1 chauffeur ajouté." : `${result.added} chauffeurs ajoutés.`}</p>}
      {result.removed > 0 && <p className="text-sm font-medium text-ink">{result.removed === 1 ? "1 chauffeur retiré du programme." : `${result.removed} chauffeurs retirés du programme.`}</p>}
      {result.failures.length > 0 && (
        <div role="alert" className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-800">
          <p className="font-medium">Non enregistrés :</p>
          <ul className="mt-1 list-disc pl-5">
            {result.failures.map((failure) => <li key={failure.driver.driverId}>{`${failure.driver.fullName} : ${failure.message}`}</li>)}
          </ul>
        </div>
      )}
    </div>
  );
}
