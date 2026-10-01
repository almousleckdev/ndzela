"use client";

import { useState } from "react";
import { Avatar } from "@/components/ui/avatar";
import { useAuthedFile } from "@/hooks/use-authed-file";
import { cn } from "@/lib/utils";
import { driversService } from "@/features/drivers/services/drivers.service";
import type { Driver } from "@/features/drivers/types";

const SIZES = { sm: "h-8 w-8", md: "h-10 w-10", xl: "h-28 w-28" };

/** The driver's profile picture (a protected file), or initials while it loads or when absent. `version` forces a reload after a change. */
export function DriverAvatar({ driver, size = "md", version = 0 }: { driver: Driver; size?: keyof typeof SIZES; version?: number }) {
  const url = useAuthedFile(driver.files.photo ? driversService.filePath(driver.id, "photo") : null, version);
  const [broken, setBroken] = useState(false);
  const name = `${driver.personal.firstName} ${driver.personal.lastName}`;
  if (!url || broken) return <Avatar name={name} size={size} />;
  // eslint-disable-next-line @next/next/no-img-element
  return <img src={url} alt="" onError={() => setBroken(true)} className={cn("shrink-0 rounded-full object-cover", SIZES[size])} />;
}
