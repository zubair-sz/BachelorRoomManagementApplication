import { Outlet, useLocation } from "react-router-dom";
import { useRooms } from "../context/RoomContext";
import Navbar from "./Navbar";
import { ErrorBanner, Spinner } from "./Feedback";
import RoomSetupPage from "../pages/RoomSetupPage";

/** Navbar + page. Anyone without a room yet is shown the create/join screen instead of the page. */
export default function AppLayout() {
  const { activeRoom, loading, error, refreshRooms } = useRooms();
  const { pathname } = useLocation();
  const roomOptional = pathname.startsWith("/join/") || pathname === "/rooms/new";

  let body;
  if (loading) {
    body = <Spinner />;
  } else if (error && !activeRoom) {
    body = <ErrorBanner message={error} onRetry={() => refreshRooms()} />;
  } else if (!activeRoom && !roomOptional) {
    body = <RoomSetupPage />;
  } else {
    body = <Outlet />;
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <main className="mx-auto max-w-5xl px-4 py-6 sm:px-6">{body}</main>
    </div>
  );
}
