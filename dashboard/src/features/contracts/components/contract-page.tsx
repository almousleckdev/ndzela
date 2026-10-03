"use client";

import { useEffect, useRef, useState } from "react";
import { Download, Printer } from "lucide-react";
import { BackLink } from "@/components/layout/back-link";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";
import { PageHeader } from "@/components/ui/page-header";
import { Skeleton } from "@/components/ui/skeleton";
import { useAsync } from "@/hooks/use-async";
import { useToast } from "@/hooks/use-toast";
import { apiBlob } from "@/lib/api/api-client";
import { getErrorMessage } from "@/lib/errors";
import { RequirePermission } from "@/features/access/components/require-permission";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { contractsService } from "@/features/contracts/services/contracts.service";

const STATUS = { ACTIVE: ["Actif", "success"], TERMINATED: ["Résilié", "danger"], COMPLETED: ["Terminé", "neutral"] } as const;

function ContractContent({ id }: { id: string }) {
  const { can } = usePermissions();
  const { addToast } = useToast();
  const contract = useAsync(() => contractsService.get(id), [id]);
  const mayPrint = can(PERMISSIONS.CONTRACTS_PRINT);
  const [pdfUrl, setPdfUrl] = useState<string | null>(null);
  const [pdfError, setPdfError] = useState<unknown>(null);
  const frame = useRef<HTMLIFrameElement>(null);

  useEffect(() => {
    if (!mayPrint) return;
    let cancelled = false;
    let created: string | null = null;
    apiBlob(contractsService.pdfPath(id)).then(
      (blob) => {
        created = URL.createObjectURL(blob);
        if (cancelled) URL.revokeObjectURL(created);
        else setPdfUrl(created);
      },
      (error) => { if (!cancelled) setPdfError(error); },
    );
    return () => { cancelled = true; if (created) URL.revokeObjectURL(created); };
  }, [id, mayPrint]);

  if (contract.error) return <EmptyState title="Contrat introuvable" description={getErrorMessage(contract.error, "Il a peut-être été supprimé.")} />;
  if (!contract.data) return <Skeleton className="mx-auto h-96 max-w-5xl" />;
  const c = contract.data;

  const print = () => {
    const target = frame.current?.contentWindow;
    if (target) { target.focus(); target.print(); }
    else addToast({ title: "Erreur", description: "Le contrat n'est pas encore chargé.", type: "error" });
  };

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <BackLink href={`/drivers/${c.driverId}`} label="Fiche du chauffeur" />
      <PageHeader
        title={`Contrat ${c.contractNumber}`}
        description="Imprimez le contrat en deux exemplaires : le bailleur et le chauffeur le signent et le chauffeur recopie la mention manuscrite."
        action={(
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant={STATUS[c.status][1]}>{STATUS[c.status][0]}</Badge>
            {mayPrint && <Button onClick={print} disabled={!pdfUrl}><Printer className="mr-2 h-4 w-4" /> Imprimer</Button>}
            {mayPrint && pdfUrl && (
              <a href={pdfUrl} download={`contrat-${c.contractNumber}.pdf`}><Button variant="outline"><Download className="mr-2 h-4 w-4" /> Télécharger</Button></a>
            )}
          </div>
        )}
      />
      {!mayPrint ? (
        <EmptyState title="Impression non autorisée" description="Votre rôle ne permet pas d'afficher le document." />
      ) : pdfError ? (
        <EmptyState title="Impossible de générer le contrat" description={getErrorMessage(pdfError, "Réessayez dans un instant.")} />
      ) : !pdfUrl ? (
        <Skeleton className="h-[75vh] w-full" />
      ) : (
        <iframe ref={frame} src={pdfUrl} title={`Contrat ${c.contractNumber}`} className="h-[80vh] w-full rounded-lg border border-slate-200 bg-white" />
      )}
    </div>
  );
}

export function ContractPage({ id }: { id: string }) {
  return (
    <RequirePermission permission={PERMISSIONS.CONTRACTS_READ}>
      <ContractContent id={id} />
    </RequirePermission>
  );
}
