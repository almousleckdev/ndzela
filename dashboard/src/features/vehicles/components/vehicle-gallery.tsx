"use client";

import { useEffect, useState } from "react";
import { ImageOff } from "lucide-react";
import { Card, CardTitle } from "@/components/ui/card";
import { Dialog } from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { vehiclesService } from "@/features/vehicles/services/vehicles.service";

// The photos of a vehicle; a click enlarges one.
export function VehicleGallery({ vehicleId }: { vehicleId: string }) {
  const [urls, setUrls] = useState<string[] | null>(null);
  const [failed, setFailed] = useState(false);
  const [opened, setOpened] = useState<string | null>(null);

  useEffect(() => {
    let loaded: string[] = [];
    let cancelled = false;
    vehiclesService.imagesWithUrls(vehicleId)
      .then((images) => {
        loaded = images.map((image) => image.url);
        if (cancelled) loaded.forEach(URL.revokeObjectURL);
        else setUrls(loaded);
      })
      .catch(() => { if (!cancelled) setFailed(true); });
    return () => { cancelled = true; loaded.forEach(URL.revokeObjectURL); };
  }, [vehicleId]);

  return (
    <Card>
      <CardTitle>Photos</CardTitle>
      {failed ? (
        <p className="text-sm text-slate-500">Impossible de charger les photos.</p>
      ) : !urls ? (
        <Skeleton className="h-28 w-full" />
      ) : urls.length === 0 ? (
        <p className="flex items-center gap-2 text-sm text-slate-500"><ImageOff className="h-4 w-4" /> Aucune photo pour ce véhicule.</p>
      ) : (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          {urls.map((url) => (
            <button key={url} type="button" onClick={() => setOpened(url)} className="overflow-hidden rounded-lg border border-slate-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={url} alt="Photo du véhicule" className="aspect-[4/3] w-full object-cover transition-transform hover:scale-105" />
            </button>
          ))}
        </div>
      )}
      {opened && (
        <Dialog isOpen onClose={() => setOpened(null)} title="Photo du véhicule" className="max-w-3xl">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={opened} alt="Photo du véhicule" className="max-h-[70vh] w-full rounded-md object-contain" />
        </Dialog>
      )}
    </Card>
  );
}
