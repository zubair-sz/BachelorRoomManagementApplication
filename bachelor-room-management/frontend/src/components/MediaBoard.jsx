import { useEffect, useRef, useState } from "react";
import { createPost, deletePost, fetchPosts } from "../api/media";
import { useAuth } from "../context/AuthContext";
import { EmptyState, ErrorBanner, Spinner } from "./Feedback";
import SecureMedia from "./SecureMedia";
import { btnDanger, btnPrimary, btnSecondary, cardCls, inputCls } from "../utils/ui";
import { errorMessage, formatDateTime, initials } from "../utils/format";

const PAGE_SIZE = 20;
const MAX_BYTES = 25 * 1024 * 1024;

/** Shared board: text updates plus photos/videos (broken appliance, grocery receipt, …). */
export default function MediaBoard({ room, isAdmin }) {
  const { user } = useAuth();
  const roomId = room.id;
  const [posts, setPosts] = useState([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState("");

  const [caption, setCaption] = useState("");
  const [file, setFile] = useState(null);
  const [posting, setPosting] = useState(false);
  const [formError, setFormError] = useState("");
  const fileInput = useRef(null);

  async function loadFirstPage() {
    setError("");
    try {
      const first = await fetchPosts(roomId, 0, PAGE_SIZE);
      setPosts(first);
      setPage(0);
      setHasMore(first.length === PAGE_SIZE);
    } catch (err) {
      setError(errorMessage(err, "Couldn't load the board."));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    setLoading(true);
    loadFirstPage();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roomId]);

  async function loadMore() {
    setLoadingMore(true);
    try {
      const next = await fetchPosts(roomId, page + 1, PAGE_SIZE);
      setPosts((prev) => [...prev, ...next.filter((p) => !prev.some((q) => q.id === p.id))]);
      setPage(page + 1);
      setHasMore(next.length === PAGE_SIZE);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoadingMore(false);
    }
  }

  function handleFile(event) {
    const chosen = event.target.files?.[0] ?? null;
    setFormError("");
    if (chosen && chosen.size > MAX_BYTES) {
      setFormError("That file is too large (25 MB maximum).");
      event.target.value = "";
      return setFile(null);
    }
    setFile(chosen);
  }

  async function handlePost(event) {
    event.preventDefault();
    setFormError("");
    if (!caption.trim() && !file) return setFormError("Write something or attach a photo or video.");
    setPosting(true);
    try {
      await createPost(roomId, { caption: caption.trim(), file });
      setCaption("");
      setFile(null);
      if (fileInput.current) fileInput.current.value = "";
      await loadFirstPage();
    } catch (err) {
      setFormError(errorMessage(err));
    } finally {
      setPosting(false);
    }
  }

  async function handleDelete(post) {
    if (!window.confirm("Delete this post?")) return;
    try {
      await deletePost(roomId, post.id);
      setPosts((prev) => prev.filter((p) => p.id !== post.id));
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <div className="space-y-5">
      <form onSubmit={handlePost} className={cardCls}>
        <ErrorBanner message={formError} />
        <textarea
          className={inputCls}
          rows={3}
          value={caption}
          onChange={(e) => setCaption(e.target.value)}
          placeholder="Share an update with your roommates…"
          maxLength={1000}
          aria-label="Post text"
        />
        <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <input ref={fileInput} type="file" accept="image/jpeg,image/png,image/gif,image/webp,video/mp4,video/webm,video/quicktime" onChange={handleFile} className="text-sm text-gray-600" aria-label="Attach photo or video" />
            {file && (
              <button type="button" className={btnSecondary} onClick={() => { setFile(null); if (fileInput.current) fileInput.current.value = ""; }}>
                Clear
              </button>
            )}
          </div>
          <button type="submit" disabled={posting} className={btnPrimary}>{posting ? "Posting…" : "Post"}</button>
        </div>
      </form>

      <ErrorBanner message={error} onRetry={loadFirstPage} />
      {loading ? (
        <Spinner />
      ) : !posts.length ? (
        <EmptyState>Nothing on the board yet. Post the first update.</EmptyState>
      ) : (
        <ul className="space-y-4">
          {posts.map((post) => (
            <li key={post.id} className={cardCls}>
              <div className="mb-2 flex items-center justify-between gap-3">
                <div className="flex items-center gap-3">
                  <span className="flex h-8 w-8 items-center justify-center rounded-full bg-brand-100 text-xs font-medium text-brand-700">
                    {initials(post.userName)}
                  </span>
                  <div>
                    <p className="text-sm font-medium text-gray-900">{post.userName}</p>
                    <p className="text-xs text-gray-500">{formatDateTime(post.postedAt)}</p>
                  </div>
                </div>
                {(post.userId === user?.id || isAdmin) && (
                  <button type="button" onClick={() => handleDelete(post)} className={btnDanger}>Delete</button>
                )}
              </div>
              {post.caption && <p className="mb-3 whitespace-pre-wrap text-sm text-gray-800">{post.caption}</p>}
              {post.fileUrl && <SecureMedia url={post.fileUrl} kind={post.fileType} alt={post.caption || "Shared photo"} />}
            </li>
          ))}
        </ul>
      )}

      {hasMore && (
        <div className="text-center">
          <button type="button" disabled={loadingMore} onClick={loadMore} className={btnSecondary}>
            {loadingMore ? "Loading…" : "Load older posts"}
          </button>
        </div>
      )}
    </div>
  );
}
