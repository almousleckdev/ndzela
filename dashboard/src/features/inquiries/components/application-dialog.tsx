"use client";

import { toOptions } from "@/lib/options";
import { useState } from "react";
import { ExternalLink, MessageCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { SelectField } from "@/components/ui/select-field";
import { Status } from "@/components/data-table/cells";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { formatDateTime } from "@/lib/time";
import { whatsappLink } from "@/lib/whatsapp";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { APPLICATION_STATUS_LABELS, APPLICATION_STATUS_TONE } from "@/features/inquiries/lib/labels";
import { inquiriesService } from "@/features/inquiries/services/inquiries.service";
import type { Application, ApplicationFileKind, ApplicationStatus } from "@/features/inquiries/types";

const FILES: { kind: ApplicationFileKind; label: string }[] = [
  { kind: "photo", label: "Photo" },
  { kind: "licence", label: "Permis de conduire" },
  { kind: "id", label: "Pièce d'identité" },
  { kind: "address-proof", label: "Casier judiciaire" },
];

const STATUS_OPTIONS = toOptions(APPLICATION_STATUS_LABELS);

// `datetime-local` wants the local time without a zone.
function toLocalInput(iso: string): string {
  const date = new Date(iso);
  const pad = (value: number) => String(value).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

// The answer already given to the candidate, as a message that can be re-sent.
function answerText(application: Application): string | null {
  if (application.status === "ACCEPTED" && application.interviewAt) {
    const when = new Date(application.interviewAt).toLocaleString("fr-FR", { dateStyle: "full", timeStyle: "short" });
    return `Bonjour ${application.firstName}, votre candidature a été retenue. Nous vous recevons pour un entretien le ${when}.${application.candidateMessage ? ` ${application.candidateMessage}` : ""}`;
  }
  if (application.status === "REJECTED" && application.candidateMessage) {
    return `Bonjour ${application.firstName}, nous ne pouvons pas donner suite à votre candidature pour le moment. ${application.candidateMessage}`;
  }
  return null;
}

function Row({ label, value }: { label: string; value: string | null }) {
  return <div className="flex justify-between gap-4 py-1.5 text-sm"><dt className="text-muted">{label}</dt><dd className="text-right font-medium">{value || "-"}</dd></div>;
}

interface ApplicationDialogProps {
  application: Application;
  onClose: () => void;
  onSaved: () => void;
}

// One application
export function ApplicationDialog({ application, onClose, onSaved }: ApplicationDialogProps) {
  const { can } = usePermissions();
  const { addToast } = useToast();
  const [status, setStatus] = useState<ApplicationStatus>(application.status);
  const [note, setNote] = useState(application.staffNote ?? "");
  const [interview, setInterview] = useState(application.interviewAt ? toLocalInput(application.interviewAt) : "");
  const [candidateMessage, setCandidateMessage] = useState(application.candidateMessage ?? "");
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string>();

  const decides = status === "ACCEPTED" || status === "REJECTED";
  const answer = answerText(application);

  const open = async (kind: ApplicationFileKind) => {
    try {
      window.open(URL.createObjectURL(await inquiriesService.applicationFile(application.id, kind)), "_blank", "noopener");
    } catch (failure) {
      addToast({ title: "Erreur", description: getErrorMessage(failure, "Impossible d'ouvrir le document."), type: "error" });
    }
  };

  const save = async () => {
    if (status === "ACCEPTED" && (!interview || new Date(interview) <= new Date())) return setError("Choisissez une date et une heure d'entretien à venir.");
    if (status === "REJECTED" && !candidateMessage.trim()) return setError("Indiquez le motif du refus : il est envoyé au candidat.");
    setIsSaving(true);
    try {
      await inquiriesService.updateApplication(application.id, { status, note, interviewAt: status === "ACCEPTED" ? new Date(interview).toISOString() : undefined, candidateMessage: decides ? candidateMessage : undefined });
      addToast({ title: "Succès", description: "Candidature mise à jour.", type: "success" });
      onSaved();
    } catch (failure) {
      setError(getErrorMessage(failure, "Impossible de mettre à jour la candidature."));
      setIsSaving(false);
    }
  };

  return (
    <Dialog isOpen onClose={onClose} title={`${application.firstName} ${application.lastName}`} description={`Reçue le ${formatDateTime(application.createdAt)}`} className="max-w-xl">
      <div className="space-y-5">
        <div className="flex items-center justify-between">
          <Status tone={APPLICATION_STATUS_TONE[application.status]}>{APPLICATION_STATUS_LABELS[application.status]}</Status>
          <a href={whatsappLink(application.phone)} target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-2 text-sm font-medium text-green-700 hover:underline"><MessageCircle className="h-4 w-4" /> WhatsApp</a>
        </div>
        <dl className="divide-y divide-slate-100">
          <Row label="Téléphone" value={application.phone} />
          <Row label="E-mail" value={application.email} />
          <Row label="Date de naissance" value={application.birthDate} />
          <Row label="Ville" value={application.city} />
          <Row label="Adresse" value={application.address} />
          <Row label="Numéro du permis" value={application.licenceNumber} />
        </dl>
        {answer && (
          <div className="rounded-lg border border-slate-200 p-3 text-sm">
            <p className="font-medium">Réponse donnée au candidat</p>
            <p className="mt-1 whitespace-pre-line text-slate-600">{answer}</p>
            <a href={whatsappLink(application.phone, answer)} target="_blank" rel="noopener noreferrer" className="mt-2 inline-flex items-center gap-2 font-medium text-green-700 hover:underline"><MessageCircle className="h-4 w-4" /> Envoyer par WhatsApp</a>
          </div>
        )}
        {application.message && <p className="rounded-lg bg-slate-50 p-3 text-sm">{application.message}</p>}

        <div>
          <p className="mb-2 text-sm font-medium">Documents</p>
          <div className="flex flex-wrap gap-2">
            {FILES.filter((file) => file.kind !== "address-proof" || application.hasAddressProof).map((file) => (
              <Button key={file.kind} variant="outline" size="sm" onClick={() => open(file.kind)}><ExternalLink className="mr-2 h-3.5 w-3.5" />{file.label}</Button>
            ))}
          </div>
        </div>

        {can(PERMISSIONS.INQUIRIES_MANAGE) && (
          <div className="space-y-4 border-t border-slate-200 pt-4">
            <SelectField label="Suivi" options={STATUS_OPTIONS} value={status} onChange={(event) => { setStatus(event.target.value as ApplicationStatus); setError(undefined); }} />
            {status === "ACCEPTED" && (
              <div>
                <Label htmlFor="application-interview">Date et heure de l&apos;entretien *</Label>
                <input id="application-interview" type="datetime-local" value={interview} onChange={(event) => { setInterview(event.target.value); setError(undefined); }}
                  className="h-10 w-full rounded-lg border border-slate-300 bg-white px-3 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20" />
              </div>
            )}
            {decides && (
              <div>
                <Label htmlFor="application-answer">{status === "ACCEPTED" ? "Message au candidat (adresse, documents à apporter)" : "Motif du refus *"}</Label>
                <textarea id="application-answer" rows={2} maxLength={500} value={candidateMessage} onChange={(event) => { setCandidateMessage(event.target.value); setError(undefined); }}
                  className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20" />
                <p className="mt-1 text-xs text-slate-500">{application.email ? `Envoyé par e-mail à ${application.email} dès l'enregistrement.` : "Le candidat n'a pas laissé d'e-mail : envoyez-lui la réponse par WhatsApp après l'enregistrement."}</p>
              </div>
            )}
            <div>
              <Label htmlFor="application-note">Note interne</Label>
              <textarea id="application-note" rows={2} maxLength={500} value={note} onChange={(event) => setNote(event.target.value)}
                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20" />
            </div>
            {error && <p role="alert" className="text-xs font-medium text-red-600">{error}</p>}
            <div className="flex justify-end gap-3">
              <Button variant="outline" onClick={onClose} disabled={isSaving}>Fermer</Button>
              <Button onClick={save} isLoading={isSaving}>Enregistrer</Button>
            </div>
          </div>
        )}
      </div>
    </Dialog>
  );
}
