import { useCallback, useEffect, useRef, useState } from "react";
import { errorMessage } from "../utils/format";

/**
 * Loads data on mount (and when `deps` change). `reload()` refetches without flashing the
 * spinner. Components that depend on a room are given `key={room.id}` so state resets on switch.
 */
export function useFetch(fetcher, deps = []) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const loadedOnce = useRef(false);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const load = useCallback(async () => {
    if (!loadedOnce.current) setLoading(true);
    setError("");
    try {
      setData(await fetcher());
      loadedOnce.current = true;
    } catch (err) {
      setError(errorMessage(err, "Couldn't load this. Please try again."));
    } finally {
      setLoading(false);
    }
  }, deps);

  useEffect(() => {
    load();
  }, [load]);

  return { data, loading, error, reload: load };
}
