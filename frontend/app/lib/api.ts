const J = { "Content-Type": "application/json" } as const;

// 共通エラー整形
async function toError(r: Response) {
  let msg = `HTTP ${r.status}`;
  try {
    const data = await r.json();
    msg = data?.error || data?.message || msg;
  } catch {}
  return new Error(msg);
}

/** 認証系（BFF /api/auth/* を叩く） */
export async function signup(username: string, password: string) {
  const r = await fetch("/api/auth/signup", {
    method: "POST",
    headers: J,
    body: JSON.stringify({ username, password }),
  });
  if (!r.ok) throw await toError(r);
  // token は返さない前提（BFF が httpOnly Cookie をセット）
  return r.json(); // 例: { authenticated: true }
}

export async function login(username: string, password: string) {
  const r = await fetch("/api/auth/login", {
    method: "POST",
    headers: J,
    body: JSON.stringify({ username, password }),
  });
  if (!r.ok) throw await toError(r);
  return r.json(); // 例: { authenticated: true }
}

export async function me() {
  const r = await fetch("/api/me", { cache: "no-store" });
  if (!r.ok) throw await toError(r);
  return r.json() as Promise<{ username: string }>;
}

/** 投稿作成（BFF /api/posts） */
export async function createPost(content: string) {
  const r = await fetch("/api/posts", {
    method: "POST",
    headers: J,
    body: JSON.stringify({ content }),
  });
  if (!r.ok) throw await toError(r);
  return r.json();
}

/** フォロー/解除（BFF /api/follows/:id） */
export async function follow(targetId: number) {
  const r = await fetch(`/api/follows/${targetId}`, { method: "POST" });
  if (!r.ok) throw await toError(r);
}

export async function unfollow(targetId: number) {
  const r = await fetch(`/api/follows/${targetId}`, { method: "DELETE" });
  if (!r.ok) throw await toError(r);
}

/** タイムライン（BFF /api/timeline） */
export type Post = {
  id: number | string;
  user: { id: number | string; username: string } | null;
  content: string;
  createdAt?: string;
};
export type TimelineRes = { items: Post[]; nextCursor?: string | null };

export async function fetchTimeline(cursor: string | null, size = 20) {
  const url = new URL("/api/timeline", typeof window === "undefined" ? "http://localhost" : window.location.origin);
  url.searchParams.set("size", String(size));
  if (cursor) url.searchParams.set("cursor", cursor);
  const r = await fetch(url, { cache: "no-store" });
  if (!r.ok) throw await toError(r);
  return r.json() as Promise<TimelineRes>;
}

export type User = { id: number | string; username: string };

export async function getPost(id: string | number) {
  const r = await fetch(`/api/posts/${id}`, { cache: "no-store" });
  if (!r.ok) throw await toError(r);
  return r.json() as Promise<{ id: number|string; user: User|null; content: string; createdAt?: string }>;
}

export async function deletePost(id: string | number) {
  const r = await fetch(`/api/posts/${id}`, { method: "DELETE" });
  if (!r.ok) throw await toError(r);
}

export async function getUser(id: string | number) {
  const r = await fetch(`/api/users/${id}`, { cache: "no-store" });
  if (!r.ok) throw await toError(r);
  return r.json() as Promise<User>;
}

export type UserPostsRes = { items: Post[]; nextCursor?: string|null };

export async function fetchUserPosts(userId: string|number, cursor: string|null, size = 20) {
  const url = new URL(`/api/users/${userId}/posts`, typeof window === "undefined" ? "http://localhost" : window.location.origin);
  url.searchParams.set("size", String(size));
  if (cursor) url.searchParams.set("cursor", cursor);
  const r = await fetch(url, { cache: "no-store" });
  if (!r.ok) throw await toError(r);
  return r.json() as Promise<UserPostsRes>;
}