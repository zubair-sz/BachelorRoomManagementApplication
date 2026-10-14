import { btnSecondary } from "../utils/ui";

export function Spinner({ label = "Loading…" }) {
  return (
    <div className="flex items-center justify-center gap-2 py-10 text-sm text-gray-400" role="status">
      <span className="h-4 w-4 animate-spin rounded-full border-2 border-gray-300 border-t-brand-600" />
      {label}
    </div>
  );
}

export function ErrorBanner({ message, onRetry }) {
  if (!message) return null;
  return (
    <div
      className="mb-4 flex items-center justify-between gap-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600"
      role="alert"
    >
      <span>{message}</span>
      {onRetry && (
        <button type="button" onClick={onRetry} className={btnSecondary}>
          Retry
        </button>
      )}
    </div>
  );
}

export function EmptyState({ children }) {
  return <p className="rounded-lg bg-gray-50 px-4 py-6 text-center text-sm text-gray-500">{children}</p>;
}

export function Badge({ tone = "gray", children }) {
  const tones = {
    gray: "bg-gray-100 text-gray-600",
    green: "bg-green-50 text-green-700",
    red: "bg-red-50 text-red-600",
    amber: "bg-amber-50 text-amber-700",
    blue: "bg-brand-50 text-brand-700",
  };
  return (
    <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${tones[tone]}`}>
      {children}
    </span>
  );
}
