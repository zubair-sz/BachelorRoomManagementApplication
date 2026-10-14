import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { fetchMyRooms } from "../api/rooms";
import { errorMessage } from "../utils/format";
import { useAuth } from "./AuthContext";

const RoomContext = createContext(null);

export function RoomProvider({ children }) {
  const { user } = useAuth();
  const [rooms, setRooms] = useState([]);
  const [activeId, setActiveId] = useState(() => localStorage.getItem("brm_room"));
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  /** Reloads the user's rooms. Pass a room id to switch to it (e.g. right after creating/joining). */
  const refreshRooms = useCallback(
    async (preferId) => {
      if (!user) {
        setRooms([]);
        setLoading(false);
        return [];
      }
      try {
        const list = await fetchMyRooms();
        setRooms(list);
        setError("");
        setActiveId((current) => {
          const wanted = preferId || current;
          return list.some((room) => room.id === wanted) ? wanted : list[0]?.id ?? null;
        });
        return list;
      } catch (err) {
        setError(errorMessage(err, "Couldn't load your rooms."));
        return [];
      } finally {
        setLoading(false);
      }
    },
    [user]
  );

  useEffect(() => {
    setLoading(true);
    refreshRooms();
  }, [refreshRooms]);

  useEffect(() => {
    if (activeId) localStorage.setItem("brm_room", activeId);
    else localStorage.removeItem("brm_room");
  }, [activeId]);

  const value = useMemo(() => {
    const activeRoom = rooms.find((room) => room.id === activeId) ?? null;
    return {
      rooms,
      activeRoom,
      isAdmin: Boolean(activeRoom?.admin),
      loading,
      error,
      setActiveRoomId: setActiveId,
      refreshRooms,
    };
  }, [rooms, activeId, loading, error, refreshRooms]);

  return <RoomContext.Provider value={value}>{children}</RoomContext.Provider>;
}

export function useRooms() {
  const ctx = useContext(RoomContext);
  if (!ctx) {
    throw new Error("useRooms must be used within a RoomProvider");
  }
  return ctx;
}
