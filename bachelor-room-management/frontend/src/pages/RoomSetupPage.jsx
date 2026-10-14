import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { createRoom, joinRoom } from "../api/rooms";
import { useRooms } from "../context/RoomContext";
import { ErrorBanner } from "../components/Feedback";
import { btnPrimary, cardCls, inputCls, labelCls } from "../utils/ui";
import { errorMessage } from "../utils/format";

/** Shown to anyone without a room, and at /rooms/new for people who want another one. */
export default function RoomSetupPage() {
  const { refreshRooms } = useRooms();
  const navigate = useNavigate();
  const [name, setName] = useState("");
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState("");
  const [error, setError] = useState("");

  async function handleCreate(event) {
    event.preventDefault();
    setError("");
    setBusy("create");
    try {
      const room = await createRoom(name);
      await refreshRooms(room.id);
      navigate("/dashboard");
    } catch (err) {
      setError(errorMessage(err, "Couldn't create the room."));
    } finally {
      setBusy("");
    }
  }

  async function handleJoin(event) {
    event.preventDefault();
    setError("");
    setBusy("join");
    try {
      const room = await joinRoom(code);
      await refreshRooms(room.id);
      navigate("/dashboard");
    } catch (err) {
      setError(errorMessage(err, "Couldn't join that room."));
    } finally {
      setBusy("");
    }
  }

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="text-xl font-semibold text-gray-900">Set up your room</h1>
      <p className="mb-6 mt-1 text-sm text-gray-500">
        Start a new room as its admin, or join your roommates with an invite code.
      </p>
      <ErrorBanner message={error} />

      <div className="grid gap-5 md:grid-cols-2">
        <form onSubmit={handleCreate} className={cardCls}>
          <h2 className="mb-3 font-medium text-gray-900">Create a room</h2>
          <label className={labelCls} htmlFor="room-name">Room name</label>
          <input
            id="room-name"
            className={inputCls}
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Flat 4B"
            maxLength={150}
            required
          />
          <button type="submit" disabled={busy !== ""} className={`${btnPrimary} mt-4 w-full`}>
            {busy === "create" ? "Creating…" : "Create room"}
          </button>
        </form>

        <form onSubmit={handleJoin} className={cardCls}>
          <h2 className="mb-3 font-medium text-gray-900">Join with a code</h2>
          <label className={labelCls} htmlFor="invite-code">Invite code</label>
          <input
            id="invite-code"
            className={inputCls}
            value={code}
            onChange={(e) => setCode(e.target.value)}
            placeholder="a1b2c3d4e5f6"
            required
          />
          <button type="submit" disabled={busy !== ""} className={`${btnPrimary} mt-4 w-full`}>
            {busy === "join" ? "Joining…" : "Join room"}
          </button>
        </form>
      </div>
    </div>
  );
}
