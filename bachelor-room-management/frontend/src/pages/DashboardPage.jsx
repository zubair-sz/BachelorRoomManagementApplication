import { Link } from "react-router-dom";
import { fetchBalances } from "../api/expenses";
import { fetchPayments } from "../api/payments";
import { fetchTasks } from "../api/tasks";
import { Badge, EmptyState, ErrorBanner, Spinner } from "../components/Feedback";
import { useAuth } from "../context/AuthContext";
import { useRooms } from "../context/RoomContext";
import { useFetch } from "../hooks/useFetch";
import { cardCls } from "../utils/ui";
import { formatDate, formatMoney, todayISO } from "../utils/format";

export default function DashboardPage() {
  const { user } = useAuth();
  const { activeRoom: room, isAdmin } = useRooms();
  const balances = useFetch(() => fetchBalances(room.id), [room.id]);
  const tasks = useFetch(() => fetchTasks(room.id, true), [room.id]);
  const payments = useFetch(() => fetchPayments(room.id), [room.id]);

  if (balances.loading || tasks.loading || payments.loading) return <Spinner />;

  const mine = balances.data?.find((b) => b.userId === user?.id);
  const net = Number(mine?.net ?? 0);
  const today = todayISO();
  const openTasks = (tasks.data ?? []).filter((t) => t.status === "PENDING");
  const overdue = openTasks.filter((t) => t.dueDate && t.dueDate < today).length;
  const waitingForMe = (payments.data ?? []).filter(
    (p) => p.status === "PENDING" && p.fromUserId !== user?.id && (isAdmin || p.toUserId === user?.id)
  ).length;

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-xl font-semibold text-gray-900">{room.name}</h1>
        <p className="mt-1 text-sm text-gray-500">
          Welcome back, {user?.name}. {room.memberCount} {room.memberCount === 1 ? "person" : "people"} in this room, admin{" "}
          {room.adminName}.
        </p>
      </div>

      <ErrorBanner
        message={balances.error || tasks.error || payments.error}
        onRetry={() => { balances.reload(); tasks.reload(); payments.reload(); }}
      />

      <div className="grid gap-4 sm:grid-cols-3">
        <div className={cardCls}>
          <p className="text-xs font-medium uppercase tracking-wide text-gray-500">Your balance</p>
          <p className={`mt-2 text-2xl font-semibold ${net < 0 ? "text-red-600" : net > 0 ? "text-green-700" : "text-gray-900"}`}>
            {net === 0 ? "Settled" : `${net > 0 ? "+" : "−"}${formatMoney(Math.abs(net))}`}
          </p>
          <p className="mt-1 text-xs text-gray-500">
            {net < 0 ? "You owe the room" : net > 0 ? "The room owes you" : "Nothing owed either way"}
          </p>
        </div>
        <div className={cardCls}>
          <p className="text-xs font-medium uppercase tracking-wide text-gray-500">Your open tasks</p>
          <p className="mt-2 text-2xl font-semibold text-gray-900">{openTasks.length}</p>
          <p className="mt-1 text-xs text-gray-500">{overdue > 0 ? <Badge tone="red">{overdue} overdue</Badge> : "None overdue"}</p>
        </div>
        <div className={cardCls}>
          <p className="text-xs font-medium uppercase tracking-wide text-gray-500">Payments to review</p>
          <p className="mt-2 text-2xl font-semibold text-gray-900">{waitingForMe}</p>
          <p className="mt-1 text-xs text-gray-500">{waitingForMe > 0 ? "Waiting for you to confirm" : "Nothing waiting"}</p>
        </div>
      </div>

      <div className={cardCls}>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="font-medium text-gray-900">Up next for you</h2>
          <Link to="/member" className="text-sm font-medium text-brand-600 hover:underline">All my tasks</Link>
        </div>
        {!openTasks.length ? (
          <EmptyState>You're all caught up.</EmptyState>
        ) : (
          <ul className="divide-y divide-gray-100">
            {openTasks.slice(0, 5).map((task) => (
              <li key={task.id} className="flex items-center justify-between gap-3 py-2 text-sm">
                <span className="text-gray-900">{task.title}</span>
                <span className="text-xs text-gray-500">{task.dueDate ? `due ${formatDate(task.dueDate)}` : "no due date"}</span>
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="flex flex-wrap gap-3 text-sm">
        <Link to="/board" className="font-medium text-brand-600 hover:underline">Open the board</Link>
        <Link to="/member" className="font-medium text-brand-600 hover:underline">Member panel</Link>
        {isAdmin && <Link to="/admin" className="font-medium text-brand-600 hover:underline">Admin panel</Link>}
      </div>
    </div>
  );
}
