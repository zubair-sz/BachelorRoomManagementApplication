import apiClient from "./client";

const data = (response) => response.data;

export const fetchMyRooms = () => apiClient.get("/rooms").then(data);
export const createRoom = (name) => apiClient.post("/rooms", { name }).then(data);
export const joinRoom = (inviteCode) => apiClient.post("/rooms/join", { inviteCode }).then(data);
export const regenerateInvite = (roomId) =>
  apiClient.post(`/rooms/${roomId}/invite/regenerate`).then(data);

export const fetchMembers = (roomId) => apiClient.get(`/rooms/${roomId}/members`).then(data);
export const addMemberByEmail = (roomId, email) =>
  apiClient.post(`/rooms/${roomId}/members`, { email }).then(data);
export const removeMember = (roomId, userId) =>
  apiClient.delete(`/rooms/${roomId}/members/${userId}`);
export const leaveRoom = (roomId) => apiClient.post(`/rooms/${roomId}/leave`);
