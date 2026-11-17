"use client";

import Link from "next/link";
import { Card, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAsync } from "@/hooks/use-async";
import { formatFcfa } from "@/lib/money";
import { contractsService } from "@/features/contracts/services/contracts.service";
import { vehiclesService } from "@/features/vehicles/services/vehicles.service";

const date = (iso: string) => new Date(iso).toLocaleDateString("fr-FR");
const STATUS: Record<string, string> = { ACTIVE: "En cours", TERMINATED: "Clôturé", COMPLETED: "Terminé" };

// Every contract the driver had, with the car of each: what stays after a resignation.
export function DriverHistoryCard({ driverId }: { driverId: string }) {
  const history = useAsync(async () => {
    const contracts = await contractsService.ofDriver(driverId);
    const ids = [...new Set(contracts.map((c) => c.vehicleId))];
    const vehicles = await Promise.all(ids.map((id) => vehiclesService.get(id).catch(() => null)));
    return contracts.map((contract) => ({ contract, vehicle: vehicles[ids.indexOf(contract.vehicleId)] }));
  }, [driverId]);

  return (
    <Card>
      <CardTitle>Contrats et véhicules</CardTitle>
      {history.isLoading && !history.data ? <Skeleton className="h-16 w-full" /> : !history.data || history.data.length === 0 ? (
        <p className="text-sm text-slate-500">Aucun contrat.</p>
      ) : (
        <ul className="divide-y divide-slate-100 text-sm">
          {history.data.map(({ contract, vehicle }) => (
            <li key={contract.id} className="space-y-1 py-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <Link href={`/contracts/${contract.id}`} className="font-medium text-primary hover:underline">{contract.contractNumber}</Link>
                <span className="text-xs text-slate-500">{STATUS[contract.status] ?? contract.status}</span>
              </div>
              <p className="text-slate-700">{vehicle ? `${vehicle.fleetNumber} · ${vehicle.brand} ${vehicle.model} · ${vehicle.plate}` : "Véhicule indisponible"}</p>
              <p className="text-xs text-slate-500">
                Du {date(contract.startDate)} {contract.endedAt ? `au ${date(contract.endedAt)}` : `(fin prévue ${date(contract.endDate)})`} · loyer {formatFcfa(contract.dailyRent)}
                {contract.endReason ? ` · motif : ${contract.endReason}` : ""}
              </p>
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}
