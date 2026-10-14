import apiClient from "./client";

const data = (response) => response.data;

export const fetchPayments = (roomId) => apiClient.get(`/rooms/${roomId}/payments`).then(data);
export const logPayment = (roomId, payload) =>
  apiClient.post(`/rooms/${roomId}/payments`, payload).then(data);
export const confirmPayment = (roomId, paymentId) =>
  apiClient.post(`/rooms/${roomId}/payments/${paymentId}/confirm`).then(data);
export const rejectPayment = (roomId, paymentId) =>
  apiClient.post(`/rooms/${roomId}/payments/${paymentId}/reject`).then(data);
export const cancelPayment = (roomId, paymentId) =>
  apiClient.delete(`/rooms/${roomId}/payments/${paymentId}`);
