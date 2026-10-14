import apiClient from "./client";

const data = (response) => response.data;

export const fetchTasks = (roomId, mine = false) =>
  apiClient.get(`/rooms/${roomId}/tasks`, { params: { mine } }).then(data);
export const createTask = (roomId, payload) =>
  apiClient.post(`/rooms/${roomId}/tasks`, payload).then(data);
export const setTaskStatus = (roomId, taskId, status) =>
  apiClient.patch(`/rooms/${roomId}/tasks/${taskId}/status`, { status }).then(data);
export const deleteTask = (roomId, taskId) => apiClient.delete(`/rooms/${roomId}/tasks/${taskId}`);

export const fetchChores = (roomId) => apiClient.get(`/rooms/${roomId}/chores`).then(data);
export const createChore = (roomId, payload) =>
  apiClient.post(`/rooms/${roomId}/chores`, payload).then(data);
export const setChoreActive = (roomId, choreId, active) =>
  apiClient.patch(`/rooms/${roomId}/chores/${choreId}`, { active }).then(data);
export const deleteChore = (roomId, choreId) =>
  apiClient.delete(`/rooms/${roomId}/chores/${choreId}`);
