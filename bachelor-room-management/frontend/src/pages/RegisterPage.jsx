import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import FormField from "../components/FormField";

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const returnTo = useLocation().state?.from || "/dashboard";
  const [form, setForm] = useState({ name: "", email: "", password: "" });
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  function handleChange(e) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setFieldErrors({});
    setSubmitting(true);
    try {
      await register(form);
      navigate(returnTo, { replace: true });
    } catch (err) {
      setFieldErrors(err.response?.data?.details || {});
      setError(err.response?.data?.message || "Unable to create your account.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4">
      <div className="w-full max-w-sm rounded-2xl bg-white p-8 shadow-sm ring-1 ring-gray-100">
        <h1 className="mb-1 text-2xl font-semibold text-gray-900">Create your account</h1>
        <p className="mb-6 text-sm text-gray-500">Start managing your room together</p>

        {error && (
          <div className="mb-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{error}</div>
        )}

        <form onSubmit={handleSubmit}>
          <FormField
            label="Full name"
            name="name"
            value={form.name}
            onChange={handleChange}
            placeholder="Jordan Lee"
            error={fieldErrors.name}
            required
          />
          <FormField
            label="Email"
            type="email"
            name="email"
            value={form.email}
            onChange={handleChange}
            placeholder="you@example.com"
            error={fieldErrors.email}
            required
          />
          <FormField
            label="Password"
            type="password"
            name="password"
            value={form.password}
            onChange={handleChange}
            placeholder="At least 8 characters"
            error={fieldErrors.password}
            required
          />
          <button
            type="submit"
            disabled={submitting}
            className="mt-2 w-full rounded-lg bg-brand-600 py-2.5 text-sm font-medium text-white transition hover:bg-brand-700 disabled:opacity-60"
          >
            {submitting ? "Creating account…" : "Create account"}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-gray-500">
          Already have an account?{" "}
          <Link to="/login" state={{ from: returnTo }} className="font-medium text-brand-600 hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
