import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { joinRoom } from "../api/rooms";
import { useRooms } from "../context/RoomContext";
import { ErrorBanner, Spinner } from "../components/Feedback";
import { errorMessage } from "../utils/format";

/** Handles invite links like /join/<code>. Logged-out visitors are sent to sign in and bounced back. */
export default function JoinByLinkPage() {
  const { code } = useParams();
  const { refreshRooms } = useRooms();
  const navigate = useNavigate();
  const [error, setError] = useState("");
  const started = useRef(false);

  useEffect(() => {
    if (started.current) return; // React StrictMode runs effects twice in development
    started.current = true;
    joinRoom(code)
      .then(async (room) => {
        await refreshRooms(room.id);
        navigate("/dashboard", { replace: true });
      })
      .catch((err) => setError(errorMessage(err, "Couldn't join that room.")));
  }, [code, navigate, refreshRooms]);

  if (error) {
    return (
      <div className="mx-auto max-w-md">
        <ErrorBanner message={error} />
        <Link to="/dashboard" className="text-sm font-medium text-brand-600 hover:underline">
          Back to dashboard
        </Link>
      </div>
    );
  }
  return <Spinner label="Joining room…" />;
}
