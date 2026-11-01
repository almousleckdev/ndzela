"use client";

import { useState } from "react";
import { AlertTriangle, Mail, MessageCircle, Phone, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
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

const REPLY_LINK = "inline-flex h-9 items-center gap-2 rounded-lg border border-slate-300 bg-white px-3 text-sm font-medium text-slate-800 hover:bg-slate-50";

interface MessageDialogProps {
  message: ContactMessage;
  onClose: () => void;
  onSaved: () => void;
  onDeleted: () => void;
}

// One message in full, with the ways to answer the visitor (e-mail sent by the server, WhatsApp, phone).
export function MessageDialog({ message, onClose, onSaved, onDeleted }: MessageDialogProps) {
  const { can } = usePermissions();
  const { addToast } = useToast();
  const [reply, setReply] = useState(message.replyMessage ?? `Bonjour ${message.name},\n\nMerci pour votre message : « ${message.subject} ».\n`);
  const [isSending, setIsSending] = useState(false);
  const [isHandling, setIsHandling] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);

  const sendReply = async () => {
    if (!reply.trim()) return;
    setIsSending(true);
    try {
      await inquiriesService.replyToMessage(message.id, reply.trim());
      addToast({ title: "Succès", description: "Réponse envoyée par e-mail.", type: "success" });
      onSaved();
    } catch (failure) {
      addToast({ title: "Erreur", description: getErrorMessage(failure, "Impossible d'envoyer la réponse."), type: "error" });
      setIsSending(false);
    }
  };

  const handle = async () => {
    setIsHandling(true);
    try {
      await inquiriesService.handleMessage(message.id);
      addToast({ title: "Succès", description: "Message marqué comme traité.", type: "success" });
      onSaved();
    } catch (failure) {
      addToast({ title: "Erreur", description: getErrorMessage(failure, "Impossible de mettre à jour le message."), type: "error" });
      setIsHandling(false);
    }
  };

  const deleteMessage = async () => {
    setIsDeleting(true);
    try {
      await inquiriesService.deleteMessage(message.id);
      addToast({ title: "Succès", description: "Message supprimé.", type: "success" });
      onDeleted();
    } catch (failure) {
      addToast({ title: "Erreur", description: getErrorMessage(failure, "Impossible de supprimer le message."), type: "error" });
      setIsDeleting(false);
      setConfirmingDelete(false);
    }
  };

  const canManage = can(PERMISSIONS.INQUIRIES_MANAGE);

  return (
    <>
    <Dialog isOpen onClose={onClose} title={message.subject} description={`${message.name}, le ${formatDateTime(message.createdAt)}`} className="max-w-xl">
      <div className="space-y-5">
        <Status tone={MESSAGE_STATUS_TONE[message.status]}>{MESSAGE_STATUS_LABELS[message.status]}</Status>
        <p className="whitespace-pre-line rounded-lg bg-slate-50 p-4 text-sm leading-relaxed">{message.message}</p>
        <dl className="divide-y divide-slate-100 text-sm">
          <div className="flex justify-between py-1.5"><dt className="text-slate-500">Téléphone</dt><dd className="font-medium">{message.phone || "-"}</dd></div>
          <div className="flex justify-between py-1.5"><dt className="text-slate-500">E-mail</dt><dd className="font-medium">{message.email || "-"}</dd></div>
        </dl>

        {message.repliedAt && (
          <div className="rounded-lg border border-slate-200 p-3 text-sm">
            <p className="font-medium">Réponse envoyée le {formatDateTime(message.repliedAt)}{message.repliedBy ? ` par ${message.repliedBy}` : ""}</p>
            <p className="mt-1 whitespace-pre-line text-slate-600">{message.replyMessage}</p>
          </div>
        )}
        {message.replyFailed && (
          <div className="flex items-start gap-2 rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm text-amber-800">
            <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
            <p>L&apos;e-mail n&apos;a pas pu être envoyé : le visiteur n&apos;a pas reçu cette réponse. Renvoyez-la, ou contactez-le par WhatsApp ou téléphone.</p>
          </div>
        )}

        {canManage && message.email && (
          <div>
            <p className="mb-2 text-sm font-medium">Répondre par e-mail</p>
            <textarea rows={4} maxLength={2000} value={reply} onChange={(event) => setReply(event.target.value)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20" />
            <p className="mt-1 text-xs text-slate-500">Envoyé à {message.email} dès l&apos;enregistrement.</p>
          </div>
        )}

        <div>
          <p className="mb-2 text-sm font-medium">Autres moyens de contact</p>
          <div className="flex flex-wrap gap-2">
            {message.phone && <a className={REPLY_LINK} href={whatsappLink(message.phone, reply)} target="_blank" rel="noopener noreferrer"><MessageCircle className="h-4 w-4 text-green-600" /> WhatsApp</a>}
            {message.phone && <a className={REPLY_LINK} href={`tel:${message.phone.replace(/\s/g, "")}`}><Phone className="h-4 w-4" /> Appeler</a>}
            {!message.email && !message.phone && <p className="text-sm text-slate-500">Le visiteur n&apos;a laissé ni e-mail ni téléphone : il ne peut pas être contacté.</p>}
          </div>
        </div>

        <div className="flex items-center justify-between gap-3 border-t border-slate-200 pt-4">
          {canManage ? (
            <Button variant="ghost" className="text-red-600 hover:bg-red-50 hover:text-red-700" onClick={() => setConfirmingDelete(true)}>
              <Trash2 className="mr-2 h-4 w-4" /> Supprimer
            </Button>
          ) : <span />}
          <div className="flex gap-3">
            <Button variant="outline" onClick={onClose}>Fermer</Button>
            {canManage && message.status === "NEW" && <Button variant="outline" onClick={handle} isLoading={isHandling}>Marquer traité</Button>}
            {canManage && message.email && (
              <Button onClick={sendReply} isLoading={isSending} disabled={!reply.trim()}>
                <Mail className="mr-2 h-4 w-4" /> Envoyer la réponse
              </Button>
            )}
          </div>
        </div>
      </div>
    </Dialog>
    <ConfirmDialog isOpen={confirmingDelete} onClose={() => setConfirmingDelete(false)} onConfirm={deleteMessage} isLoading={isDeleting} destructive
      title="Supprimer ce message ?" description={`Le message de ${message.name} sera supprimé définitivement.`}
      confirmText="Supprimer" cancelText="Annuler" />
    </>
  );
}
