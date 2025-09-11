"use client";
import useSWRInfinite from "swr/infinite";
import { useState } from "react";

type Post = {
  id: number | string;
  user: { id: number | string; username: string } | null;
  content: string;
  createdAt?: string;
};

type TimelineRes = { items: Post[]; nextCursor?: string | null };

const PAGE_SIZE = 20;

const fetcher = (url: string) =>
  fetch(url, { cache: "no-store" }).then((r) => {
    if (!r.ok) throw new Error("timeline failed");
    return r.json() as Promise<TimelineRes>;
  });

export default function TimelinePage() {
  const getKey = (index: number, prev: TimelineRes | null) => {
    if (prev && !prev.nextCursor && index > 0) return null; // 終端
    const u = new URL("/api/timeline", window.location.origin);
    u.searchParams.set("size", String(PAGE_SIZE));
    if (index > 0 && prev?.nextCursor) u.searchParams.set("cursor", prev.nextCursor);
    return u.toString();
  };

  const { data, error, size, setSize, isValidating, mutate } = useSWRInfinite<TimelineRes>(getKey, fetcher);
  const items = data?.flatMap((d) => d.items) ?? [];
  const isEnd = data && data[data.length - 1]?.nextCursor == null;

  const [content, setContent] = useState("");

  async function onCreatePost(e: React.FormEvent) {
    e.preventDefault();
    const text = content.trim();
    if (!text || text.length > 280) return alert("1〜280文字で入力してください");

    // 送信
    const r = await fetch("/api/posts", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ content: text }),
    });
    if (!r.ok) return alert("投稿に失敗しました");

    setContent("");
    // 先頭を最新にするため再取得
    await mutate();
  }

  return (
    <main className="max-w-xl mx-auto p-4 space-y-6">
      <form onSubmit={onCreatePost} className="space-y-2">
        <textarea
          className="w-full border p-2"
          placeholder="いまどうしてる？（最大280文字）"
          maxLength={280}
          value={content}
          onChange={(e) => setContent(e.target.value)}
        />
        <div className="flex justify-end">
          <button className="bg-black text-white px-4 py-2 rounded disabled:opacity-60" disabled={!content.trim()}>
            投稿
          </button>
        </div>
      </form>

      {error && <p className="text-red-600">読み込みに失敗しました</p>}

      <ul className="space-y-4">
        {items.map((p) => (
          <li key={p.id} className="border p-3 rounded">
            <div className="text-sm text-gray-600">
              {p.user?.username ?? "unknown"}・{p.createdAt ? new Date(p.createdAt).toLocaleString() : ""}
            </div>
            <p className="whitespace-pre-wrap mt-1">{p.content}</p>
          </li>
        ))}
      </ul>

      <div className="flex justify-center py-4">
        {!isEnd ? (
          <button
            onClick={() => setSize(size + 1)}
            disabled={isValidating}
            className="px-4 py-2 border rounded"
          >
            {isValidating ? "読み込み中..." : "さらに読み込む"}
          </button>
        ) : (
          <span className="text-gray-500">これ以上ありません</span>
        )}
      </div>
    </main>
  );
}
