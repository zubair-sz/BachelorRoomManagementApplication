const currency = import.meta.env.VITE_CURRENCY || "INR";
const moneyFormatter = new Intl.NumberFormat(undefined, { style: "currency", currency });

export const formatMoney = (value) => moneyFormatter.format(Number(value ?? 0));

/** Dates from the API are plain yyyy-mm-dd; parse them as local dates so they never shift a day. */
export function formatDate(isoDate) {
  if (!isoDate) return "—";
  return new Date(`${isoDate}T00:00:00`).toLocaleDateString(undefined, {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

export function formatDateTime(isoInstant) {
  if (!isoInstant) return "";
  return new Date(isoInstant).toLocaleString(undefined, {
    day: "numeric",
    month: "short",
    hour: "numeric",
    minute: "2-digit",
  });
}

/** Today as yyyy-mm-dd in the browser's own time zone (toISOString would use UTC). */
export function todayISO() {
  const now = new Date();
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
}

export const errorMessage = (err, fallback = "Something went wrong. Please try again.") =>
  err?.response?.data?.message || fallback;

export const WEEKDAYS = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
];

export const weekdayLabel = (day) => day.charAt(0) + day.slice(1).toLowerCase();

export const initials = (name = "") =>
  name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join("");
