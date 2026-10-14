import { useState } from "react";
import { Navigate } from "react-router-dom";
import { fetchPayments } from "../api/payments";
import ExpenseTracker from "../components/ExpenseTracker";
import MembersPanel from "../components/MembersPanel";
import PaymentsPanel from "../components/PaymentsPanel";
import TaskScheduler from "../components/TaskScheduler";
import Tabs from "../components/Tabs";
import { useRooms } from "../context/RoomContext";
import { useFetch } from "../hooks/useFetch";

/** Admin Panel: manage people, money and chores for the active room. Only reachable by that room's admin. */
export default function AdminPanelPage() {
  const { activeRoom: room, isAdmin } = useRooms();
  const [tab, setTab] = useState("expenses");
  const payments = useFetch(() => fetchPayments(room.id), [room.id, tab]);

  if (!isAdmin) return <Navigate to="/dashboard" replace />;

  const pending = (payments.data ?? []).filter((p) => p.status === "PENDING").length;
  const tabs = [
    { id: "expenses", label: "Expenses" },
    { id: "payments", label: "Payments", badge: pending },
    { id: "tasks", label: "Tasks & chores" },
    { id: "members", label: "Members" },
  ];

  return (
    <div>
      <h1 className="mb-1 text-xl font-semibold text-gray-900">Admin panel</h1>
      <p className="mb-5 text-sm text-gray-500">Managing {room.name}</p>
      <Tabs tabs={tabs} active={tab} onChange={setTab} />

      {/* key={room.id} resets each section's state when you switch rooms */}
      {tab === "expenses" && <ExpenseTracker key={room.id} room={room} isAdmin />}
      {tab === "payments" && <PaymentsPanel key={room.id} room={room} isAdmin />}
      {tab === "tasks" && <TaskScheduler key={room.id} room={room} isAdmin />}
      {tab === "members" && <MembersPanel key={room.id} room={room} isAdmin />}
    </div>
  );
}
