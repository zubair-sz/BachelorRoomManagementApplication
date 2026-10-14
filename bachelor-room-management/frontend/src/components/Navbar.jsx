import { useState } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useRooms } from "../context/RoomContext";
import { Badge } from "./Feedback";
import { btnSecondary } from "../utils/ui";

const NEW_ROOM = "__new__";

export default function Navbar() {
  const { user, logout } = useAuth();
  const { rooms, activeRoom, isAdmin, setActiveRoomId } = useRooms();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);

  const links = [
    { to: "/dashboard", label: "Dashboard" },
    { to: "/board", label: "Board" },
    { to: "/member", label: "Member Panel" },
    ...(isAdmin ? [{ to: "/admin", label: "Admin Panel" }] : []),
  ];

  const linkClass = ({ isActive }) =>
    `block rounded-lg px-3 py-2 text-sm font-medium transition ${
      isActive ? "bg-brand-50 text-brand-700" : "text-gray-600 hover:bg-gray-100"
    }`;

  function handleRoomChange(event) {
    const value = event.target.value;
    if (value === NEW_ROOM) {
      navigate("/rooms/new");
    } else {
      setActiveRoomId(value);
    }
    setOpen(false);
  }

  return (
    <nav className="sticky top-0 z-10 bg-white shadow-sm">
      <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3 sm:px-6">
        <div className="flex items-center gap-6">
          <Link to="/dashboard" className="text-lg font-semibold text-brand-600">
            Room Mgmt
          </Link>
          {activeRoom && (
            <ul className="hidden items-center gap-1 md:flex">
              {links.map((link) => (
                <li key={link.to}>
                  <NavLink to={link.to} className={linkClass}>
                    {link.label}
                  </NavLink>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="hidden items-center gap-3 md:flex">
          {activeRoom && (
            <>
              <select
                value={activeRoom.id}
                onChange={handleRoomChange}
                aria-label="Switch room"
                className="rounded-lg border border-gray-200 bg-white px-2 py-1.5 text-sm text-gray-700"
              >
                {rooms.map((room) => (
                  <option key={room.id} value={room.id}>
                    {room.name}
                  </option>
                ))}
                <option value={NEW_ROOM}>＋ New or join room…</option>
              </select>
              <Badge tone={isAdmin ? "blue" : "gray"}>{isAdmin ? "Admin" : "Member"}</Badge>
            </>
          )}
          <span className="text-sm text-gray-600">{user?.name}</span>
          <button type="button" onClick={logout} className={btnSecondary}>
            Log out
          </button>
        </div>

        <button
          type="button"
          onClick={() => setOpen((value) => !value)}
          aria-label="Toggle menu"
          aria-expanded={open}
          className="rounded-lg border border-gray-200 p-2 text-gray-600 md:hidden"
        >
          <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2">
            {open ? <path d="M6 6l12 12M18 6L6 18" /> : <path d="M4 7h16M4 12h16M4 17h16" />}
          </svg>
        </button>
      </div>

      {open && (
        <div className="border-t border-gray-100 px-4 pb-4 pt-2 md:hidden">
          {activeRoom && (
            <>
              <ul className="space-y-1">
                {links.map((link) => (
                  <li key={link.to}>
                    <NavLink to={link.to} className={linkClass} onClick={() => setOpen(false)}>
                      {link.label}
                    </NavLink>
                  </li>
                ))}
              </ul>
              <select
                value={activeRoom.id}
                onChange={handleRoomChange}
                aria-label="Switch room"
                className="mt-3 w-full rounded-lg border border-gray-200 bg-white px-2 py-2 text-sm text-gray-700"
              >
                {rooms.map((room) => (
                  <option key={room.id} value={room.id}>
                    {room.name}
                  </option>
                ))}
                <option value={NEW_ROOM}>＋ New or join room…</option>
              </select>
            </>
          )}
          <div className="mt-3 flex items-center justify-between">
            <span className="text-sm text-gray-600">
              {user?.name} {activeRoom && <Badge tone={isAdmin ? "blue" : "gray"}>{isAdmin ? "Admin" : "Member"}</Badge>}
            </span>
            <button type="button" onClick={logout} className={btnSecondary}>
              Log out
            </button>
          </div>
        </div>
      )}
    </nav>
  );
}
