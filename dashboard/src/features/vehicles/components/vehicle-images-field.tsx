"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { ImagePlus, Loader2, X } from "lucide-react";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { vehiclesService } from "@/features/vehicles/services/vehicles.service";
import type { VehicleImage } from "@/features/vehicles/types";

const MAX_IMAGES = 8;
const ACCEPTED = "image/jpeg,image/png,image/webp";

interface Thumb {
  key: string;
  url: string;
  // Present once the image is saved server-side; absent while it's only a local, not-yet-uploaded selection.
  remoteId?: string;
}

interface VehicleImagesFieldProps {
  // null while adding a new vehicle (no id yet): selections stay local until the vehicle is created.
  vehicleId: string | null;
  pendingFiles: File[];
  onPendingFilesChange: (files: File[]) => void;
}

// One or more photos of the vehicle. Editing an existing vehicle uploads/deletes immediately;
// adding a new one keeps the files local until the vehicle itself is saved.
export function VehicleImagesField({ vehicleId, pendingFiles, onPendingFilesChange }: VehicleImagesFieldProps) {
  const { addToast } = useToast();
  const input = useRef<HTMLInputElement>(null);
  const [remote, setRemote] = useState<(VehicleImage & { url: string })[]>([]);
  const [isLoading, setIsLoading] = useState(vehicleId !== null);
  const [isUploading, setIsUploading] = useState(false);

  useEffect(() => {
    if (!vehicleId) return;
    let cancelled = false;
    (async () => {
      try {
        const list = await vehiclesService.images(vehicleId);
        const withUrls = await Promise.all(list.map(async (image) => ({ ...image, url: URL.createObjectURL(await vehiclesService.imageBlob(vehicleId, image.id)) })));
        if (!cancelled) setRemote(withUrls);
      } catch (error) {
        if (!cancelled) addToast({ title: "Erreur", description: getErrorMessage(error, "Impossible de charger les photos."), type: "error" });
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    })();
    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [vehicleId]);

  const localThumbs: Thumb[] = useMemo(() => pendingFiles.map((file, i) => ({ key: `local-${i}`, url: URL.createObjectURL(file) })), [pendingFiles]);
  const remoteThumbs: Thumb[] = remote.map((image) => ({ key: image.id, url: image.url, remoteId: image.id }));
  const thumbs = vehicleId ? remoteThumbs : localThumbs;
  const count = thumbs.length;

  const pick = async (chosen: FileList | null) => {
    if (!chosen || chosen.length === 0) return;
    const files = Array.from(chosen);
    if (count + files.length > MAX_IMAGES) {
      addToast({ title: "Erreur", description: `Un véhicule peut avoir au maximum ${MAX_IMAGES} photos.`, type: "error" });
      return;
    }
    if (vehicleId) {
      setIsUploading(true);
      try {
        const updated = await vehiclesService.uploadImages(vehicleId, files);
        const withUrls = await Promise.all(updated.map(async (image) => ({ ...image, url: URL.createObjectURL(await vehiclesService.imageBlob(vehicleId, image.id)) })));
        setRemote(withUrls);
      } catch (error) {
        addToast({ title: "Erreur", description: getErrorMessage(error, "Impossible d'ajouter la photo."), type: "error" });
      } finally {
        setIsUploading(false);
      }
    } else {
      onPendingFilesChange([...pendingFiles, ...files]);
    }
  };

  const removeLocal = (index: number) => onPendingFilesChange(pendingFiles.filter((_, i) => i !== index));

  const removeRemote = async (imageId: string) => {
    if (!vehicleId) return;
    try {
      await vehiclesService.deleteImage(vehicleId, imageId);
      setRemote((current) => current.filter((image) => image.id !== imageId));
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, "Impossible de supprimer la photo."), type: "error" });
    }
  };

  return (
    <div>
      <Label>Photos du véhicule</Label>
      <div className="flex flex-wrap gap-3">
        {isLoading && <Loader2 className="h-5 w-5 animate-spin text-slate-400" />}
        {thumbs.map((thumb, i) => (
          <div key={thumb.key} className="group relative h-20 w-20">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={thumb.url} alt="" className="h-20 w-20 rounded-md border border-slate-200 object-cover" />
            <button type="button" aria-label="Retirer la photo" onClick={() => (thumb.remoteId ? removeRemote(thumb.remoteId) : removeLocal(i))}
              className="absolute -right-2 -top-2 flex h-6 w-6 items-center justify-center rounded-full bg-slate-900 text-white shadow hover:bg-slate-700">
              <X className="h-3.5 w-3.5" />
            </button>
          </div>
        ))}
        {count < MAX_IMAGES && (
          <button type="button" onClick={() => input.current?.click()} disabled={isUploading}
            className="flex h-20 w-20 flex-col items-center justify-center gap-1 rounded-md border border-dashed border-slate-300 text-slate-500 hover:bg-slate-50">
            {isUploading ? <Loader2 className="h-5 w-5 animate-spin" /> : <ImagePlus className="h-5 w-5" />}
            <span className="text-[11px]">Ajouter</span>
          </button>
        )}
      </div>
      <input ref={input} type="file" accept={ACCEPTED} multiple className="hidden" onChange={(event) => { pick(event.target.files); event.target.value = ""; }} />
      <p className="mt-1 text-xs text-slate-500">JPG, PNG ou WebP, 5 Mo maximum chacune, {MAX_IMAGES} au plus.</p>
    </div>
  );
}
