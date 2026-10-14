import { useMemo, useState } from "react";
import { createExpense, deleteExpense, fetchBalances, fetchExpenses } from "../api/expenses";
import { fetchMembers } from "../api/rooms";
import { useFetch } from "../hooks/useFetch";
import { useAuth } from "../context/AuthContext";
import { Badge, EmptyState, ErrorBanner, Spinner } from "./Feedback";
import { btnDanger, btnPrimary, cardCls, inputCls, labelCls } from "../utils/ui";
import { errorMessage, formatDate, formatMoney, todayISO } from "../utils/format";

/**
 * Expense Tracker: a ledger of shared expenses plus everyone's running balance. Admins can add
 * expenses (equal split between chosen people, or custom amounts) and delete them; members get
 * a read-only view of the same ledger and their own balance.
 */
export default function ExpenseTracker({ room, isAdmin }) {
  const { user } = useAuth();
  const roomId = room.id;
  const expenses = useFetch(() => fetchExpenses(roomId), [roomId]);
  const balances = useFetch(() => fetchBalances(roomId), [roomId]);
  const members = useFetch(() => fetchMembers(roomId), [roomId]);
  const [actionError, setActionError] = useState("");

  const refreshAll = () => Promise.all([expenses.reload(), balances.reload()]);

  async function handleDelete(expense) {
    if (!window.confirm(`Delete "${expense.description}"? Balances will be recalculated.`)) return;
    setActionError("");
    try {
      await deleteExpense(roomId, expense.id);
      await refreshAll();
    } catch (err) {
      setActionError(errorMessage(err));
    }
  }

  if (expenses.loading || balances.loading || members.loading) return <Spinner />;
  const loadError = expenses.error || balances.error || members.error;

  return (
    <div className="space-y-5">
      <ErrorBanner
        message={loadError || actionError}
        onRetry={loadError ? () => { expenses.reload(); balances.reload(); members.reload(); } : undefined}
      />

      <BalanceTable balances={balances.data ?? []} currentUserId={user?.id} />

      {isAdmin && members.data && (
        <ExpenseForm roomId={roomId} members={members.data} currentUserId={user?.id} onCreated={refreshAll} />
      )}

      <div className={cardCls}>
        <h3 className="mb-3 font-medium text-gray-900">Expense ledger</h3>
        {!expenses.data?.length ? (
          <EmptyState>No expenses yet.{isAdmin ? " Add the first one above." : ""}</EmptyState>
        ) : (
          <ul className="divide-y divide-gray-100">
            {expenses.data.map((expense) => {
              const myShare = expense.shares.find((share) => share.userId === user?.id);
              return (
                <li key={expense.id} className="py-3">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="text-sm font-medium text-gray-900">{expense.description}</p>
                      <p className="text-xs text-gray-500">
                        {formatDate(expense.date)} · paid by {expense.paidByName} ·{" "}
                        {expense.splitType === "EQUAL" ? "split equally" : "custom split"}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="text-sm font-semibold text-gray-900">{formatMoney(expense.amount)}</p>
                      {myShare && <p className="text-xs text-gray-500">Your share {formatMoney(myShare.amount)}</p>}
                    </div>
                  </div>
                  <p className="mt-1 text-xs text-gray-400">
                    {expense.shares.map((share) => `${share.name} ${formatMoney(share.amount)}`).join(" · ")}
                  </p>
                  {isAdmin && (
                    <button type="button" onClick={() => handleDelete(expense)} className={`${btnDanger} mt-2`}>
                      Delete
                    </button>
                  )}
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </div>
  );
}

function BalanceTable({ balances, currentUserId }) {
  return (
    <div className={cardCls}>
      <h3 className="mb-1 font-medium text-gray-900">Balances</h3>
      <p className="mb-3 text-xs text-gray-500">
        Positive means the room owes them. Negative means they owe the room. Only confirmed payments count.
      </p>
      <div className="overflow-x-auto">
        <table className="w-full min-w-[32rem] text-sm">
          <thead>
            <tr className="text-left text-xs text-gray-500">
              <th className="pb-2 font-medium">Person</th>
              <th className="pb-2 text-right font-medium">Paid</th>
              <th className="pb-2 text-right font-medium">Share</th>
              <th className="pb-2 text-right font-medium">Settled</th>
              <th className="pb-2 text-right font-medium">Balance</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {balances.map((b) => {
              const net = Number(b.net);
              const settled = Number(b.paymentsSent) - Number(b.paymentsReceived);
              return (
                <tr key={b.userId} className={b.userId === currentUserId ? "bg-brand-50/50" : ""}>
                  <td className="py-2 pr-2 text-gray-900">{b.name}{b.userId === currentUserId && " (you)"}</td>
                  <td className="py-2 text-right text-gray-600">{formatMoney(b.paid)}</td>
                  <td className="py-2 text-right text-gray-600">{formatMoney(b.owed)}</td>
                  <td className="py-2 text-right text-gray-600">{formatMoney(settled)}</td>
                  <td className="py-2 text-right">
                    {net === 0 ? <Badge tone="green">Settled</Badge> : (
                      <span className={net > 0 ? "font-semibold text-green-700" : "font-semibold text-red-600"}>
                        {net > 0 ? "+" : "−"}{formatMoney(Math.abs(net))}
                      </span>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function ExpenseForm({ roomId, members, currentUserId, onCreated }) {
  const everyone = useMemo(() => members.map((m) => m.userId), [members]);
  const [description, setDescription] = useState("");
  const [amount, setAmount] = useState("");
  const [date, setDate] = useState(todayISO());
  const [paidBy, setPaidBy] = useState(currentUserId);
  const [splitType, setSplitType] = useState("EQUAL");
  const [participants, setParticipants] = useState(() => new Set(everyone));
  const [custom, setCustom] = useState({});
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const total = Number(amount) || 0;
  const customSum = Object.values(custom).reduce((sum, v) => sum + (Number(v) || 0), 0);
  const customDiff = Math.round((total - customSum) * 100) / 100;

  function toggleParticipant(id) {
    setParticipants((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");

    const payload = {
      description,
      amount: total,
      date,
      paidBy,
      splitType,
    };
    if (splitType === "EQUAL") {
      if (participants.size === 0) return setError("Pick at least one person to split between.");
      payload.participantIds = [...participants];
    } else {
      if (customDiff !== 0) return setError("The shares must add up to the expense amount.");
      payload.shares = everyone
        .filter((id) => Number(custom[id]) > 0)
        .map((id) => ({ userId: id, amount: Number(custom[id]) }));
    }

    setBusy(true);
    try {
      await createExpense(roomId, payload);
      setDescription("");
      setAmount("");
      setCustom({});
      setParticipants(new Set(everyone));
      await onCreated();
    } catch (err) {
      const details = err.response?.data?.details;
      setError(details ? Object.values(details).join(" ") : errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className={cardCls}>
      <h3 className="mb-3 font-medium text-gray-900">Log an expense</h3>
      <ErrorBanner message={error} />

      <div className="grid gap-3 sm:grid-cols-2">
        <div>
          <label className={labelCls} htmlFor="exp-desc">What was it for?</label>
          <input id="exp-desc" className={inputCls} value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Rent, groceries, electricity…" maxLength={255} required />
        </div>
        <div>
          <label className={labelCls} htmlFor="exp-amount">Amount</label>
          <input id="exp-amount" type="number" min="0.01" step="0.01" className={inputCls} value={amount} onChange={(e) => setAmount(e.target.value)} required />
        </div>
        <div>
          <label className={labelCls} htmlFor="exp-date">Date</label>
          <input id="exp-date" type="date" className={inputCls} value={date} onChange={(e) => setDate(e.target.value)} required />
        </div>
        <div>
          <label className={labelCls} htmlFor="exp-paid">Paid by</label>
          <select id="exp-paid" className={inputCls} value={paidBy} onChange={(e) => setPaidBy(e.target.value)}>
            {members.map((m) => <option key={m.userId} value={m.userId}>{m.name}</option>)}
          </select>
        </div>
      </div>

      <fieldset className="mt-4">
        <legend className={labelCls}>Split</legend>
        <div className="mb-3 flex gap-4 text-sm">
          <label className="flex items-center gap-2">
            <input type="radio" name="split" checked={splitType === "EQUAL"} onChange={() => setSplitType("EQUAL")} /> Equally
          </label>
          <label className="flex items-center gap-2">
            <input type="radio" name="split" checked={splitType === "CUSTOM"} onChange={() => setSplitType("CUSTOM")} /> Custom amounts
          </label>
        </div>

        {splitType === "EQUAL" ? (
          <div className="flex flex-wrap gap-2">
            {members.map((m) => (
              <label key={m.userId} className={`flex cursor-pointer items-center gap-2 rounded-full border px-3 py-1 text-sm ${participants.has(m.userId) ? "border-brand-500 bg-brand-50 text-brand-700" : "border-gray-200 text-gray-500"}`}>
                <input type="checkbox" className="sr-only" checked={participants.has(m.userId)} onChange={() => toggleParticipant(m.userId)} />
                {m.name}
              </label>
            ))}
          </div>
        ) : (
          <div className="space-y-2">
            {members.map((m) => (
              <div key={m.userId} className="flex items-center gap-3">
                <span className="w-32 truncate text-sm text-gray-700">{m.name}</span>
                <input type="number" min="0" step="0.01" aria-label={`${m.name}'s share`} className={`${inputCls} max-w-[10rem]`} value={custom[m.userId] ?? ""} onChange={(e) => setCustom((prev) => ({ ...prev, [m.userId]: e.target.value }))} />
              </div>
            ))}
            <p className={`text-xs ${customDiff === 0 && total > 0 ? "text-green-700" : "text-gray-500"}`}>
              {customDiff === 0 && total > 0 ? "Shares add up." : `${formatMoney(Math.abs(customDiff))} ${customDiff > 0 ? "left to assign" : "over the total"}`}
            </p>
          </div>
        )}
      </fieldset>

      <button type="submit" disabled={busy} className={`${btnPrimary} mt-4`}>{busy ? "Saving…" : "Add expense"}</button>
    </form>
  );
}
