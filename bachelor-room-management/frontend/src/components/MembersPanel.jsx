import { useState } from "react";
import { addMemberByEmail, fetchMembers, regenerateInvite, removeMember } from "../api/rooms";
import { useFetch } from "../hooks/useFetch";
import { useRooms } from "../context/RoomContext";
import { useAuth } from "../context/AuthContext";
import { Badge, EmptyState, ErrorBanner, Spinner } from "./Feedback";
import { btnDanger, btnPrimary, btnSecondary, cardCls, inputCls } from "../utils/ui";
import { errorMessage, formatDate, initials } from "../utils/format";

export default function MembersPanel({ room, isAdmin }) {
  const { user } = useAuth();
  const { refreshRooms } = useRooms();
  const { data: members, loading, error, reload } = useFetch(() => fetchMembers(room.id), [room.id]);
  const [email, setEmail] = useState("");
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState("");
  const [notice, setNotice] = useState("");

  const inviteLink = room.inviteCode ? `${window.location.origin}/join/${room.inviteCode}` : "";

  async function run(action, success) {
    setActionError("");
    setNotice("");
    setBusy(true);
    try {
      await action();
      if (success) setNotice(success);
      await Promise.all([reload(), refreshRooms()]);
    } catch (err) {
      setActionError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function handleAdd(event) {
    event.preventDefault();
    await run(() => addMemberByEmail(room.id, email), "Member added.");
    setEmail("");
  }

  async function copyLink() {
    try {
      await navigator.clipboard.writeText(inviteLink);
      setNotice("Invite link copied.");
    } catch {
      setNotice("Couldn't copy automatically. Select the link and copy it.");
    }
  }

  function handleRemove(member) {
    if (window.confirm(`Remove ${member.name} from ${room.name}?`)) {
      run(() => removeMember(room.id, member.userId), `${member.name} was removed.`);
    }
  }

  if (loading) return <Spinner />;

  return (
    <div className="space-y-5">
      <ErrorBanner message={error || actionError} onRetry={error ? reload : undefined} />
      {notice && <p className="rounded-lg bg-green-50 px-3 py-2 text-sm text-green-700" role="status">{notice}</p>}

      {isAdmin && (
        <div className={cardCls}>
          <h3 className="mb-3 font-medium text-gray-900">Invite roommates</h3>
          <label className="mb-1 block text-sm text-gray-600" htmlFor="invite-link">Invite link</label>
          <div className="flex flex-col gap-2 sm:flex-row">
            <input id="invite-link" readOnly value={inviteLink} className={`${inputCls} bg-gray-50`} onFocus={(e) => e.target.select()} />
            <button type="button" onClick={copyLink} className={btnSecondary}>Copy</button>
            <button
              type="button"
              disabled={busy}
              onClick={() => {
                if (window.confirm("The old link will stop working. Continue?")) {
                  run(() => regenerateInvite(room.id), "New invite link created.");
                }
              }}
              className={btnSecondary}
            >
              Reset
            </button>
          </div>

          <form onSubmit={handleAdd} className="mt-4 flex flex-col gap-2 sm:flex-row">
            <input
              type="email"
              className={inputCls}
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="Add by email (they must have an account)"
              aria-label="Roommate email"
              required
            />
            <button type="submit" disabled={busy} className={btnPrimary}>Add</button>
          </form>
        </div>
      )}

      <div className={cardCls}>
        <h3 className="mb-3 font-medium text-gray-900">Roommates ({members?.length ?? 0})</h3>
        {!members?.length ? (
          <EmptyState>No one here yet.</EmptyState>
        ) : (
          <ul className="divide-y divide-gray-100">
            {members.map((member) => (
              <li key={member.userId} className="flex items-center justify-between gap-3 py-3">
                <div className="flex min-w-0 items-center gap-3">
                  <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-brand-100 text-sm font-medium text-brand-700">
                    {initials(member.name)}
                  </span>
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-gray-900">
                      {member.name} {member.userId === user?.id && <span className="text-gray-400">(you)</span>}
                    </p>
                    <p className="truncate text-xs text-gray-500">{member.email} · joined {formatDate(member.joinedAt?.slice(0, 10))}</p>
                  </div>
                </div>
                <div className="flex shrink-0 items-center gap-2">
                  {member.admin && <Badge tone="blue">Admin</Badge>}
                  {isAdmin && !member.admin && (
                    <button type="button" disabled={busy} onClick={() => handleRemove(member)} className={btnDanger}>
                      Remove
                    </button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
