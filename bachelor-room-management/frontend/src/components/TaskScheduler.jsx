import { useState } from "react";
import { fetchMembers } from "../api/rooms";
import {
  createChore, createTask, deleteChore, deleteTask, fetchChores, fetchTasks, setChoreActive, setTaskStatus,
} from "../api/tasks";
import { useFetch } from "../hooks/useFetch";
import { useAuth } from "../context/AuthContext";
import { Badge, EmptyState, ErrorBanner, Spinner } from "./Feedback";
import { btnDanger, btnPrimary, btnSecondary, cardCls, inputCls, labelCls } from "../utils/ui";
import { WEEKDAYS, errorMessage, formatDate, todayISO, weekdayLabel } from "../utils/format";

/**
 * Task Scheduler: one-off tasks ("orders") the admin hands out, plus weekly recurring chores that
 * the server turns into tasks every midnight (optionally rotating between roommates).
 * Admins manage everything; members see their tasks and tick them off.
 */
export default function TaskScheduler({ room, isAdmin, mineOnly = false }) {
  const { user } = useAuth();
  const roomId = room.id;
  const tasks = useFetch(() => fetchTasks(roomId, mineOnly), [roomId, mineOnly]);
  const chores = useFetch(() => fetchChores(roomId), [roomId]);
  const members = useFetch(() => fetchMembers(roomId), [roomId]);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);

  async function act(id, action) {
    setError("");
    setBusyId(id);
    try {
      await action();
      await Promise.all([tasks.reload(), chores.reload()]);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusyId(null);
    }
  }

  if (tasks.loading || chores.loading || members.loading) return <Spinner />;
  const loadError = tasks.error || chores.error || members.error;
  const today = todayISO();

  return (
    <div className="space-y-5">
      <ErrorBanner
        message={loadError || error}
        onRetry={loadError ? () => { tasks.reload(); chores.reload(); members.reload(); } : undefined}
      />

      {isAdmin && members.data && (
        <div className="grid gap-5 md:grid-cols-2">
          <TaskForm roomId={roomId} members={members.data} onCreated={tasks.reload} />
          <ChoreForm roomId={roomId} members={members.data} onCreated={() => Promise.all([chores.reload(), tasks.reload()])} />
        </div>
      )}

      <div className={cardCls}>
        <h3 className="mb-3 font-medium text-gray-900">{mineOnly ? "My tasks" : "Tasks"}</h3>
        {!tasks.data?.length ? (
          <EmptyState>{mineOnly ? "Nothing assigned to you. Enjoy!" : "No tasks yet."}</EmptyState>
        ) : (
          <ul className="divide-y divide-gray-100">
            {tasks.data.map((task) => {
              const done = task.status === "DONE";
              const overdue = !done && task.dueDate && task.dueDate < today;
              const mine = task.assignedToId === user?.id;
              const canToggle = mine || isAdmin;
              return (
                <li key={task.id} className="flex items-start gap-3 py-3">
                  <input
                    type="checkbox"
                    checked={done}
                    disabled={!canToggle || busyId === task.id}
                    onChange={() => act(task.id, () => setTaskStatus(roomId, task.id, done ? "PENDING" : "DONE"))}
                    aria-label={`Mark "${task.title}" as ${done ? "not done" : "done"}`}
                    className="mt-1 h-4 w-4 rounded border-gray-300"
                  />
                  <div className="min-w-0 flex-1">
                    <p className={`text-sm font-medium ${done ? "text-gray-400 line-through" : "text-gray-900"}`}>{task.title}</p>
                    {task.description && <p className="text-xs text-gray-500">{task.description}</p>}
                    <p className="mt-1 flex flex-wrap items-center gap-2 text-xs text-gray-500">
                      <span>{task.assignedToName ? `For ${task.assignedToName}${mine ? " (you)" : ""}` : "Unassigned"}</span>
                      {task.dueDate && <span>· due {formatDate(task.dueDate)}</span>}
                      {task.recurring && <Badge tone="blue">weekly</Badge>}
                      {overdue && <Badge tone="red">overdue</Badge>}
                    </p>
                  </div>
                  {isAdmin && (
                    <button type="button" disabled={busyId === task.id} onClick={() => act(task.id, () => deleteTask(roomId, task.id))} className={btnDanger}>
                      Delete
                    </button>
                  )}
                </li>
              );
            })}
          </ul>
        )}
      </div>

      <div className={cardCls}>
        <h3 className="mb-1 font-medium text-gray-900">Weekly chores</h3>
        <p className="mb-3 text-xs text-gray-500">A task is created automatically at midnight on each chore's day.</p>
        {!chores.data?.length ? (
          <EmptyState>No recurring chores set up.</EmptyState>
        ) : (
          <ul className="divide-y divide-gray-100">
            {chores.data.map((chore) => (
              <li key={chore.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
                <div>
                  <p className={`text-sm font-medium ${chore.active ? "text-gray-900" : "text-gray-400"}`}>{chore.title}</p>
                  <p className="text-xs text-gray-500">
                    Every {weekdayLabel(chore.dayOfWeek)} ·{" "}
                    {chore.rotate ? "rotates" : "fixed"}
                    {chore.assignedToName && ` · next up: ${chore.assignedToName}`}
                  </p>
                </div>
                {isAdmin && (
                  <div className="flex items-center gap-2">
                    {!chore.active && <Badge>paused</Badge>}
                    <button type="button" disabled={busyId === chore.id} onClick={() => act(chore.id, () => setChoreActive(roomId, chore.id, !chore.active))} className={btnSecondary}>
                      {chore.active ? "Pause" : "Resume"}
                    </button>
                    <button
                      type="button"
                      disabled={busyId === chore.id}
                      onClick={() => window.confirm(`Delete the "${chore.title}" chore? Tasks already created are kept.`) && act(chore.id, () => deleteChore(roomId, chore.id))}
                      className={btnDanger}
                    >
                      Delete
                    </button>
                  </div>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

function TaskForm({ roomId, members, onCreated }) {
  const [form, setForm] = useState({ title: "", description: "", assignedTo: "", dueDate: "" });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const set = (field) => (e) => setForm((prev) => ({ ...prev, [field]: e.target.value }));

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setBusy(true);
    try {
      await createTask(roomId, {
        title: form.title,
        description: form.description || null,
        assignedTo: form.assignedTo || null,
        dueDate: form.dueDate || null,
      });
      setForm({ title: "", description: "", assignedTo: "", dueDate: "" });
      await onCreated();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className={cardCls}>
      <h3 className="mb-3 font-medium text-gray-900">Assign a task</h3>
      <ErrorBanner message={error} />
      <div className="space-y-3">
        <div>
          <label className={labelCls} htmlFor="task-title">Task</label>
          <input id="task-title" className={inputCls} value={form.title} onChange={set("title")} placeholder="Buy bin bags" maxLength={200} required />
        </div>
        <div>
          <label className={labelCls} htmlFor="task-desc">Details (optional)</label>
          <input id="task-desc" className={inputCls} value={form.description} onChange={set("description")} maxLength={2000} />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className={labelCls} htmlFor="task-who">Assign to</label>
            <select id="task-who" className={inputCls} value={form.assignedTo} onChange={set("assignedTo")}>
              <option value="">Anyone</option>
              {members.map((m) => <option key={m.userId} value={m.userId}>{m.name}</option>)}
            </select>
          </div>
          <div>
            <label className={labelCls} htmlFor="task-due">Due</label>
            <input id="task-due" type="date" className={inputCls} value={form.dueDate} onChange={set("dueDate")} />
          </div>
        </div>
      </div>
      <button type="submit" disabled={busy} className={`${btnPrimary} mt-4`}>{busy ? "Saving…" : "Assign task"}</button>
    </form>
  );
}

function ChoreForm({ roomId, members, onCreated }) {
  const [form, setForm] = useState({ title: "", dayOfWeek: "SUNDAY", assignedTo: "", rotate: true });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const set = (field) => (e) => setForm((prev) => ({ ...prev, [field]: e.target.value }));

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setBusy(true);
    try {
      await createChore(roomId, {
        title: form.title,
        dayOfWeek: form.dayOfWeek,
        assignedTo: form.assignedTo || null,
        rotate: form.rotate,
      });
      setForm({ title: "", dayOfWeek: "SUNDAY", assignedTo: "", rotate: true });
      await onCreated();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className={cardCls}>
      <h3 className="mb-3 font-medium text-gray-900">Add a weekly chore</h3>
      <ErrorBanner message={error} />
      <div className="space-y-3">
        <div>
          <label className={labelCls} htmlFor="chore-title">Chore</label>
          <input id="chore-title" className={inputCls} value={form.title} onChange={set("title")} placeholder="Clean the bathroom" maxLength={200} required />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className={labelCls} htmlFor="chore-day">Every</label>
            <select id="chore-day" className={inputCls} value={form.dayOfWeek} onChange={set("dayOfWeek")}>
              {WEEKDAYS.map((day) => <option key={day} value={day}>{weekdayLabel(day)}</option>)}
            </select>
          </div>
          <div>
            <label className={labelCls} htmlFor="chore-who">Starts with</label>
            <select id="chore-who" className={inputCls} value={form.assignedTo} onChange={set("assignedTo")}>
              <option value="">{form.rotate ? "First roommate" : "Nobody"}</option>
              {members.map((m) => <option key={m.userId} value={m.userId}>{m.name}</option>)}
            </select>
          </div>
        </div>
        <label className="flex items-center gap-2 text-sm text-gray-700">
          <input type="checkbox" checked={form.rotate} onChange={(e) => setForm((prev) => ({ ...prev, rotate: e.target.checked }))} />
          Rotate between roommates each week
        </label>
      </div>
      <button type="submit" disabled={busy} className={`${btnPrimary} mt-4`}>{busy ? "Saving…" : "Add chore"}</button>
    </form>
  );
}
