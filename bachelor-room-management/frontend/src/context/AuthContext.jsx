import { createContext, useContext, useEffect, useState } from "react";
import { loginUser, registerUser, fetchCurrentUser } from "../api/auth";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const storedUser = localStorage.getItem("brm_user");
    const token = localStorage.getItem("brm_token");

    if (token && storedUser) {
      setUser(JSON.parse(storedUser));
      // Verify token is still valid against the backend; if not, interceptor
      // in client.js will clear storage and redirect to /login on failure.
      fetchCurrentUser().catch(() => {});
    }
    setLoading(false);
  }, []);

  function persistSession(authResponse) {
    const sessionUser = {
      id: authResponse.userId,
      name: authResponse.name,
      email: authResponse.email,
      role: authResponse.role,
    };
    localStorage.setItem("brm_token", authResponse.token);
    localStorage.setItem("brm_user", JSON.stringify(sessionUser));
    setUser(sessionUser);
    return sessionUser;
  }

  async function login(credentials) {
    const authResponse = await loginUser(credentials);
    return persistSession(authResponse);
  }

  async function register(details) {
    const authResponse = await registerUser(details);
    return persistSession(authResponse);
  }

  function logout() {
    localStorage.removeItem("brm_token");
    localStorage.removeItem("brm_user");
    setUser(null);
  }

  const value = { user, loading, login, register, logout };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return ctx;
}
