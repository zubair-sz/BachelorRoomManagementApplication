import apiClient from "./client";

const data = (response) => response.data;

export const fetchPosts = (roomId, page = 0, size = 20) =>
  apiClient.get(`/rooms/${roomId}/media`, { params: { page, size } }).then(data);

export function createPost(roomId, { caption, file }) {
  const form = new FormData();
  if (caption) form.append("caption", caption);
  if (file) form.append("file", file);
  return apiClient.post(`/rooms/${roomId}/media`, form).then(data);
}

export const deletePost = (roomId, postId) => apiClient.delete(`/rooms/${roomId}/media/${postId}`);
