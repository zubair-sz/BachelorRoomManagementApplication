import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { leaveRoom } from "../api/rooms";
import ExpenseTracker from "../components/ExpenseTracker";
import { ErrorBanner } from "../components/Feedback";
import MembersPanel from "../components/MembersPanel";
import PaymentsPanel from "../components/PaymentsPanel";
import TaskScheduler from "../components/TaskScheduler";
import Tabs from "../components/Tabs";
import { useRooms } from "../context/RoomContext";
import { btnDanger } from "../utils/ui";
import { errorMessage } from "../utils/format";

/** Member Panel: what every roommate sees — their tasks, their balance and payments, the roster. */
export default function MemberPanelPage() {
  const { activeRoom: room, isAdmin, refreshRooms } = useRooms();
  const navigate = useNavigate();
  const [tab, setTab] = useState("tasks");
  const [error, setError] = useState("");

  const tabs = [
    { id: "tasks", label: "My tasks" },
    { id: "money", label: "Money" },
    { id: "ledger", label: "Expenses" },
    { id: "roster", label: "Roommates" },
  ];

  async function handleLeave() {
    if (!window.confirm(`Leave ${room.name}? You can only leave when your balance is settled.`)) return;
    setError("");
    try {
      await leaveRoom(room.id);
      await refreshRooms();
      navigate("/dashboard");
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <div>
      <div className="mb-1 flex items-center justify-between gap-3">
        <h1 className="text-xl font-semibold text-gray-900">Member panel</h1>
        {!isAdmin && <button type="button" onClick={handleLeave} className={btnDanger}>Leave room</button>}
      </div>
      <p className="mb-5 text-sm text-gray-500">Your view of {room.name}</p>
      <ErrorBanner message={error} />
      <Tabs tabs={tabs} active={tab} onChange={setTab} />

      {tab === "tasks" && <TaskScheduler key={room.id} room={room} isAdmin={false} mineOnly />}
      {tab === "money" && <PaymentsPanel key={room.id} room={room} isAdmin={isAdmin} />}
      {tab === "ledger" && <ExpenseTracker key={room.id} room={room} isAdmin={false} />}
      {tab === "roster" && <MembersPanel key={room.id} room={room} isAdmin={false} />}
    </div>
  );
}
