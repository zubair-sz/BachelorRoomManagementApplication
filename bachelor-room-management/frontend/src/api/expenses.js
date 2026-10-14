import apiClient from "./client";

const data = (response) => response.data;

export const fetchExpenses = (roomId) => apiClient.get(`/rooms/${roomId}/expenses`).then(data);
export const createExpense = (roomId, payload) =>
  apiClient.post(`/rooms/${roomId}/expenses`, payload).then(data);
export const deleteExpense = (roomId, expenseId) =>
  apiClient.delete(`/rooms/${roomId}/expenses/${expenseId}`);
export const fetchBalances = (roomId) => apiClient.get(`/rooms/${roomId}/balances`).then(data);
