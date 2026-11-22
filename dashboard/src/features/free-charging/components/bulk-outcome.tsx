import type { CreateFailure } from "@/features/free-charging/hooks/use-create-many";

interface BulkOutcomeProps {
  added: number;
  failures: CreateFailure[];
}

export function BulkOutcome({ added, failures }: BulkOutcomeProps) {
  return (
    <div role="status" className="space-y-3">
      {added > 0 && <p className="text-sm font-medium text-ink">{added === 1 ? "1 chauffeur ajouté." : `${added} chauffeurs ajoutés.`}</p>}
      {failures.length > 0 && (
        <div role="alert" className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-800">
          <p className="font-medium">Non ajoutés :</p>
          <ul className="mt-1 list-disc pl-5">
            {failures.map((failure) => <li key={failure.driver.driverId}>{`${failure.driver.fullName} : ${failure.message}`}</li>)}
          </ul>
        </div>
      )}
    </div>
  );
}
