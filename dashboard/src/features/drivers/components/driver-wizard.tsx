"use client";

import { BackLink } from "@/components/layout/back-link";
import { EmptyState } from "@/components/ui/empty-state";
import { PageHeader } from "@/components/ui/page-header";
import { Skeleton } from "@/components/ui/skeleton";
import { DriverWizardForm } from "@/features/drivers/components/driver-wizard-form";
import { useFetch } from "@/hooks/use-fetch";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { trainingCentersService } from "@/features/drivers/services/training-centers.service";
import type { Driver } from "@/features/drivers/types";
import { countriesService } from "@/features/tenancy/services/countries.service";
import { useCountrySelectionStore } from "@/features/tenancy/store/country-selection.store";
import { vehiclesService } from "@/features/vehicles/services/vehicles.service";

// Creates a driver in 3 steps plus the password pop-up; with `driver` it edits the profile instead.
export function DriverWizard({ driver }: { driver?: Driver }) {
  const me = useAuthStore((state) => state.user);
  const selectedCountry = useCountrySelectionStore((state) => state.selectedCode);
  const countries = useFetch(countriesService.list);
  const centers = useFetch(trainingCentersService.list);
  const vehicles = useFetch(() => vehiclesService.list({ status: "AVAILABLE", q: "", page: 0, size: 100 }));
  const mustPickCountry = !driver && me?.country === null && selectedCountry === null;
  const loading = countries.isLoading || centers.isLoading || vehicles.isLoading;

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <BackLink href={driver ? `/drivers/${driver.id}` : "/drivers"} label={driver ? "Fiche du chauffeur" : "Chauffeurs"} />
      <PageHeader title={driver ? "Modifier le chauffeur" : "Nouveau chauffeur"}
        description={driver ? undefined : "Le chauffeur ne crée pas son compte : il le reçoit de vous, avec son véhicule."} />
      {mustPickCountry ? (
        <EmptyState title="Choisissez un pays" description="Sélectionnez un pays dans l'en-tête pour créer un chauffeur." />
      ) : loading ? (
        <Skeleton className="h-96 w-full" />
      ) : (
        // The form is built only once the lists are loaded, so stored phone numbers can be split into code and number.
        <DriverWizardForm driver={driver} countries={countries.data} centers={centers.data ?? []} vehicles={vehicles.data?.items ?? []} />
      )}
    </div>
  );
}
