import apiClient from "./client";

export async function registerUser({ name, email, password }) {
  const { data } = await apiClient.post("/auth/register", { name, email, password });
  return data;
}

export async function loginUser({ email, password }) {
  const { data } = await apiClient.post("/auth/login", { email, password });
  return data;
}

export async function fetchCurrentUser() {
  const { data } = await apiClient.get("/users/me");
  return data;
}
