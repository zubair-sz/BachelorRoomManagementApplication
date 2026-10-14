import { useState } from "react";
import { fetchBalances } from "../api/expenses";
import { cancelPayment, confirmPayment, fetchPayments, logPayment, rejectPayment } from "../api/payments";
import { useFetch } from "../hooks/useFetch";
import { useAuth } from "../context/AuthContext";
import { Badge, EmptyState, ErrorBanner, Spinner } from "./Feedback";
import { btnDanger, btnPrimary, btnSecondary, cardCls, inputCls, labelCls } from "../utils/ui";
import { errorMessage, formatDateTime, formatMoney } from "../utils/format";

const STATUS_TONE = { PENDING: "amber", CONFIRMED: "green", REJECTED: "red" };

/**
 * Members log money they handed to the admin; the recipient (or admin) confirms it arrived.
 * Balances only change once a payment is confirmed.
 */
export default function PaymentsPanel({ room, isAdmin }) {
  const { user } = useAuth();
  const roomId = room.id;
  const payments = useFetch(() => fetchPayments(roomId), [roomId]);
  const balances = useFetch(() => fetchBalances(roomId), [roomId]);
  const [amount, setAmount] = useState("");
  const [note, setNote] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const myBalance = balances.data?.find((b) => b.userId === user?.id);
  const iOwe = myBalance && Number(myBalance.net) < 0 ? Math.abs(Number(myBalance.net)) : 0;

  async function act(action) {
    setError("");
    setBusy(true);
    try {
      await action();
      await Promise.all([payments.reload(), balances.reload()]);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function handleLog(event) {
    event.preventDefault();
    await act(async () => {
      await logPayment(roomId, { amount: Number(amount), note: note.trim() || null });
      setAmount("");
      setNote("");
    });
  }

  if (payments.loading || balances.loading) return <Spinner />;

  return (
    <div className="space-y-5">
      <ErrorBanner
        message={payments.error || balances.error || error}
        onRetry={payments.error || balances.error ? () => { payments.reload(); balances.reload(); } : undefined}
      />

      {!isAdmin && (
        <div className={cardCls}>
          <h3 className="font-medium text-gray-900">Your balance</h3>
          <p className="mt-1 text-sm text-gray-600">
            {iOwe > 0
              ? <>You owe <span className="font-semibold text-red-600">{formatMoney(iOwe)}</span>.</>
              : <>You're all settled up. {myBalance && Number(myBalance.net) > 0 && `The room owes you ${formatMoney(myBalance.net)}.`}</>}
          </p>

          <form onSubmit={handleLog} className="mt-4 grid gap-3 sm:grid-cols-[10rem_1fr_auto] sm:items-end">
            <div>
              <label className={labelCls} htmlFor="pay-amount">Amount paid to {room.adminName}</label>
              <input id="pay-amount" type="number" min="0.01" step="0.01" className={inputCls} value={amount} onChange={(e) => setAmount(e.target.value)} placeholder={iOwe > 0 ? String(iOwe) : ""} required />
            </div>
            <div>
              <label className={labelCls} htmlFor="pay-note">Note (optional)</label>
              <input id="pay-note" className={inputCls} value={note} onChange={(e) => setNote(e.target.value)} placeholder="UPI, cash…" maxLength={255} />
            </div>
            <button type="submit" disabled={busy} className={btnPrimary}>Log payment</button>
          </form>
          <p className="mt-2 text-xs text-gray-500">{room.adminName} will confirm once the money has arrived.</p>
        </div>
      )}

      <div className={cardCls}>
        <h3 className="mb-3 font-medium text-gray-900">{isAdmin ? "All payments" : "Your payments"}</h3>
        {!payments.data?.length ? (
          <EmptyState>No payments logged yet.</EmptyState>
        ) : (
          <ul className="divide-y divide-gray-100">
            {payments.data.map((p) => {
              const pending = p.status === "PENDING";
              const iAmPayer = p.fromUserId === user?.id;
              const canResolve = pending && !iAmPayer && (isAdmin || p.toUserId === user?.id);
              return (
                <li key={p.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
                  <div>
                    <p className="text-sm text-gray-900">
                      <span className="font-medium">{p.fromName}</span> paid <span className="font-medium">{p.toName}</span>{" "}
                      <span className="font-semibold">{formatMoney(p.amount)}</span>
                    </p>
                    <p className="text-xs text-gray-500">{formatDateTime(p.createdAt)}{p.note && ` · ${p.note}`}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge tone={STATUS_TONE[p.status]}>{p.status.toLowerCase()}</Badge>
                    {canResolve && (
                      <>
                        <button type="button" disabled={busy} onClick={() => act(() => confirmPayment(roomId, p.id))} className={btnPrimary}>Confirm</button>
                        <button type="button" disabled={busy} onClick={() => act(() => rejectPayment(roomId, p.id))} className={btnSecondary}>Reject</button>
                      </>
                    )}
                    {pending && iAmPayer && (
                      <button type="button" disabled={busy} onClick={() => act(() => cancelPayment(roomId, p.id))} className={btnDanger}>Cancel</button>
                    )}
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </div>
  );
}
