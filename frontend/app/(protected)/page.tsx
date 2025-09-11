"use client";
import useSWRInfinite from "swr/infinite";
import { useState } from "react";
import { createPost, fetchTimeline, type TimelineRes } from "../lib/api";
import Link from "next/link"

const PAGE_SIZE = 20;

// SWR のキーは [ "timeline", cursor ] のタプルにして薄いラッパを呼ぶ
const getKey = (index: number, prev: TimelineRes | null) => {
  if (prev && !prev.nextCursor && index > 0) return null; // 終端
  const cursor = index === 0 ? null : prev?.nextCursor ?? null;
  return ["timeline", cursor] as const;
};

// タプルキーを受け取り、第2要素 (cursor) を使って BFF を叩く
const fetcher = (args: readonly ["timeline", string | null]) => {
  const [, cursor] = args;
  return fetchTimeline(cursor, PAGE_SIZE);
};

export default function TimelinePage() {
  const { data, error, size, setSize, isValidating, mutate } =
    useSWRInfinite<TimelineRes, Error, typeof getKey>(getKey, fetcher);

  const items = data?.flatMap((d) => d.items) ?? [];
  const isEnd = data && data[data.length - 1]?.nextCursor == null;

  const [content, setContent] = useState("");

  async function onCreatePost(e: React.FormEvent) {
    e.preventDefault();
    const text = content.trim();
    if (!text || text.length > 280) return alert("1〜280文字で入力してください");
    try {
      await createPost(text);  // ← 薄いラッパ経由で /api/posts
      setContent("");
      await mutate();          // 先頭更新
    } catch (e: any) {
      alert(e?.message ?? "投稿に失敗しました");
    }
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
            <Link href={`/users/${p.user?.id ?? ""}`} className="underline">
              {p.user?.username ?? "unknown"}
            </Link>
            ・{p.createdAt ? new Date(p.createdAt).toLocaleString() : ""}
          </div>
          <p className="whitespace-pre-wrap mt-1">
            <Link href={`/posts/${p.id}`} className="hover:underline">
              {p.content}
            </Link>
          </p>
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
