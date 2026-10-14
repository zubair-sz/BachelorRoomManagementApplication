import { useEffect, useState } from "react";
import apiClient from "../api/client";

/**
 * <img>/<video> tags can't send the Authorization header, so uploads are fetched through the
 * authenticated API client and shown from a temporary blob URL.
 */
export default function SecureMedia({ url, kind, alt = "Shared photo" }) {
  const [src, setSrc] = useState(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let cancelled = false;
    let objectUrl = null;
    setSrc(null);
    setFailed(false);

    apiClient
      .get(url, { responseType: "blob" })
      .then((response) => {
        if (cancelled) return;
        objectUrl = URL.createObjectURL(response.data);
        setSrc(objectUrl);
      })
      .catch(() => {
        if (!cancelled) setFailed(true);
      });

    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [url]);

  if (failed) {
    return <p className="rounded-lg bg-gray-50 px-3 py-6 text-center text-sm text-gray-400">Couldn't load this file.</p>;
  }
  if (!src) {
    return <div className="h-48 animate-pulse rounded-lg bg-gray-100" aria-label="Loading media" />;
  }
  if (kind === "VIDEO") {
    return <video src={src} controls className="max-h-96 w-full rounded-lg bg-black" />;
  }
  return <img src={src} alt={alt} className="max-h-96 w-full rounded-lg object-contain" />;
}
