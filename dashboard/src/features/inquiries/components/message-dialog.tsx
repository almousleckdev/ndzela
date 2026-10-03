"use client";

import { useState } from "react";
import { Mail, MessageCircle, Phone } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { Status } from "@/components/data-table/cells";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { formatDateTime } from "@/lib/time";
import { whatsappLink } from "@/lib/whatsapp";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { MESSAGE_STATUS_LABELS, MESSAGE_STATUS_TONE } from "@/features/inquiries/lib/labels";
import { inquiriesService } from "@/features/inquiries/services/inquiries.service";
import type { ContactMessage } from "@/features/inquiries/types";

const REPLY = "inline-flex h-9 items-center gap-2 rounded-lg border border-slate-300 bg-white px-3 text-sm font-medium text-slate-800 hover:bg-slate-50";

interface MessageDialogProps {
  message: ContactMessage;
  onClose: () => void;
  onSaved: () => void;
}

// One message in full, with the ways to answer the visitor (e-mail, WhatsApp
export function MessageDialog({ message, onClose, onSaved }: MessageDialogProps) {
  const { can } = usePermissions();
  const { addToast } = useToast();
  const [isSaving, setIsSaving] = useState(false);
  const reply = `Bonjour ${message.name},\n\nMerci pour votre message : « ${message.subject} ».\n`;

  const handle = async () => {
    setIsSaving(true);
    try {
      await inquiriesService.handleMessage(message.id);
      addToast({ title: "Succès", description: "Message marqué comme traité.", type: "success" });
      onSaved();
    } catch (failure) {
      addToast({ title: "Erreur", description: getErrorMessage(failure, "Impossible de mettre à jour le message."), type: "error" });
      setIsSaving(false);
    }
  };

  return (
    <Dialog isOpen onClose={onClose} title={message.subject} description={`${message.name}, le ${formatDateTime(message.createdAt)}`} className="max-w-xl">
      <div className="space-y-5">
        <Status tone={MESSAGE_STATUS_TONE[message.status]}>{MESSAGE_STATUS_LABELS[message.status]}</Status>
        <p className="whitespace-pre-line rounded-lg bg-slate-50 p-4 text-sm leading-relaxed">{message.message}</p>
        <dl className="divide-y divide-slate-100 text-sm">
          <div className="flex justify-between py-1.5"><dt className="text-slate-500">Téléphone</dt><dd className="font-medium">{message.phone || "-"}</dd></div>
          <div className="flex justify-between py-1.5"><dt className="text-slate-500">E-mail</dt><dd className="font-medium">{message.email || "-"}</dd></div>
        </dl>
        <div>
          <p className="mb-2 text-sm font-medium">Répondre</p>
          <div className="flex flex-wrap gap-2">
            {message.email && <a className={REPLY} href={`mailto:${message.email}?subject=${encodeURIComponent(`Re: ${message.subject}`)}&body=${encodeURIComponent(reply)}`}><Mail className="h-4 w-4" /> E-mail</a>}
            {message.phone && <a className={REPLY} href={whatsappLink(message.phone, reply)} target="_blank" rel="noopener noreferrer"><MessageCircle className="h-4 w-4 text-green-600" /> WhatsApp</a>}
            {message.phone && <a className={REPLY} href={`tel:${message.phone.replace(/\s/g, "")}`}><Phone className="h-4 w-4" /> Appeler</a>}
            {!message.email && !message.phone && <p className="text-sm text-slate-500">Le visiteur n&apos;a laissé ni e-mail ni téléphone : il ne peut pas être contacté.</p>}
          </div>
        </div>
        <div className="flex justify-end gap-3 border-t border-slate-200 pt-4">
          <Button variant="outline" onClick={onClose} disabled={isSaving}>Fermer</Button>
          {message.status === "NEW" && can(PERMISSIONS.INQUIRIES_MANAGE) && <Button onClick={handle} isLoading={isSaving}>Marquer traité</Button>}
        </div>
      </div>
    </Dialog>
  );
}
