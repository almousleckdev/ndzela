"use client";

import { useRef, useState } from "react";
import { Camera, ShieldCheck, ShieldAlert, Trash2 } from "lucide-react";
import { Avatar } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { RoleChip } from "@/features/access/components/role-chip";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { prepareAvatar } from "@/lib/image";
import { displayName } from "@/features/profile/lib/user";
import { removePicture, uploadPicture } from "@/features/profile/services/profile.service";
import type { UserProfile } from "@/features/auth/types";

// Anything a browser can decode is fine
const MAX_SOURCE_BYTES = 25 * 1024 * 1024;

export function ProfileHero({ user }: { user: UserProfile }) {
  const { addToast } = useToast();
  const fileInput = useRef<HTMLInputElement>(null);
  const [isBusy, setIsBusy] = useState(false);

  const run = async (action: () => Promise<unknown>, success: string) => {
    setIsBusy(true);
    try {
      await action();
      addToast({ title: "Succès", description: success, type: "success" });
    } catch (error) {
      const unreadable = error instanceof Error && error.message === "IMAGE_UNREADABLE";
      addToast({
        title: "Erreur",
        description: unreadable ? "Impossible de lire cette image. Essayez un fichier PNG, JPEG ou WebP." : getErrorMessage(error, "L'opération a échoué."),
        type: "error",
      });
    } finally {
      setIsBusy(false);
    }
  };

  const onFileChosen = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;
    if (file.size > MAX_SOURCE_BYTES) {
      addToast({ title: "Image trop lourde", description: "Choisissez une image de moins de 25 Mo.", type: "error" });
      return;
    }
    run(async () => uploadPicture(await prepareAvatar(file)), "Photo de profil mise à jour.");
  };

  return (
    <section className="relative overflow-hidden rounded-2xl border border-slate-200 bg-white">
      <div className="h-24 bg-sidebar bg-[radial-gradient(circle_at_85%_-20%,rgba(249,115,22,0.55),transparent_55%)]" />
      <div className="flex flex-col gap-6 px-6 pb-6 sm:flex-row sm:items-end sm:px-8">
        <div className="-mt-14 shrink-0">
          <div className="relative w-fit">
            <Avatar name={displayName(user)} src={user.profilePictureUrl} size="xl" className="ring-4 ring-white" />
            <Button
              size="icon"
              onClick={() => fileInput.current?.click()}
              isLoading={isBusy}
              aria-label="Changer la photo de profil"
              className="absolute bottom-0 right-0 h-9 w-9 rounded-full ring-4 ring-white"
            >
              {!isBusy && <Camera className="h-4 w-4" />}
            </Button>
            <input ref={fileInput} type="file" accept="image/*" onChange={onFileChosen} className="sr-only" tabIndex={-1} aria-hidden />
          </div>
        </div>

        <div className="min-w-0 flex-1 pt-2">
          <h2 className="truncate font-display text-3xl font-semibold tracking-tight text-slate-900">{displayName(user)}</h2>
          <p className="mt-0.5 truncate text-sm text-slate-500">{user.email}</p>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            {user.roles.map((role) => <RoleChip key={role} role={role} />)}
            <span className={`inline-flex items-center gap-1.5 text-xs font-medium ${user.mfaEnabled ? "text-emerald-700" : "text-amber-700"}`}>
              {user.mfaEnabled ? <ShieldCheck className="h-4 w-4" /> : <ShieldAlert className="h-4 w-4" />}
              {user.mfaEnabled ? "Double authentification active" : "Double authentification à configurer"}
            </span>
          </div>
        </div>

        {user.profilePictureUrl && (
          <Button variant="ghost" size="sm" disabled={isBusy} onClick={() => run(removePicture, "Photo supprimée.")} className="self-start text-slate-500 sm:self-end">
            <Trash2 className="mr-1.5 h-4 w-4" /> Supprimer la photo
          </Button>
        )}
      </div>
    </section>
  );
}
