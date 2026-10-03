"use client";

import { useEffect, useState } from "react";
import { apiBlob } from "@/lib/api/api-client";

// An object URL for a protected file, or null while loading, when absent or when it cannot be read.
export function useAuthedFile(path: string | null, version = 0): string | null {
  const [loaded, setLoaded] = useState<{ key: string; url: string } | null>(null);
  const key = path ? `${path}#${version}` : null;

  useEffect(() => {
    if (!path || !key) return;
    let cancelled = false;
    let created: string | null = null;
    apiBlob(path)
      .then((blob) => {
        created = URL.createObjectURL(blob);
        if (cancelled) URL.revokeObjectURL(created);
        else setLoaded({ key, url: created });
      })
      .catch(() => undefined);
    return () => {
      cancelled = true;
      if (created) URL.revokeObjectURL(created);
    };
  }, [path, key]);

  return loaded && loaded.key === key ? loaded.url : null;
}
