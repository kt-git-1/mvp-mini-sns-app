import { getUser, fetchUserPosts, type UserPostsRes } from "../../../lib/api";
import Link from "next/link";

export default async function UserPage({ params }: { params: { id: string } }) {
  const user = await getUser(params.id);
  return (
    <main className="max-w-xl mx-auto p-4 space-y-6">
      <header className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">@{user.username}</h1>
        <FollowButton targetId={user.id} />
      </header>
      <UserPostsList userId={String(user.id)} />
    </main>
  );
}

// ----- クライアント部分 -----
"use client";
import useSWRInfinite from "swr/infinite";
import { useState } from "react";

function UserPostsList({ userId }: { userId: string }) {
  const PAGE_SIZE = 20;
  const getKey = (index: number, prev: UserPostsRes | null) => {
    if (prev && !prev.nextCursor && index > 0) return null;
    const cursor = index === 0 ? null : prev?.nextCursor ?? null;
    return ["user-posts", userId, cursor] as const;
  };
  const fetcher = (args: readonly ["user-posts", string, string | null]) => {
    const [, _userId, cursor] = args;
    return fetchUserPosts(userId, cursor, PAGE_SIZE);
  };

  const { data, error, size, setSize, isValidating } =
    useSWRInfinite<UserPostsRes, Error, typeof getKey>(getKey, fetcher);

  const items = data?.flatMap((d) => d.items) ?? [];
  const isEnd = data && data[data.length - 1]?.nextCursor == null;

  return (
    <>
      {error && <p className="text-red-600">読み込みに失敗しました</p>}
      <ul className="space-y-4">
        {items.map((p) => (
          <li key={String(p.id)} className="border p-3 rounded">
            <div className="text-sm text-gray-600">
              <Link href={`/posts/${p.id}`} className="underline">投稿詳細</Link>・{p.createdAt ? new Date(p.createdAt).toLocaleString() : ""}
            </div>
            <p className="whitespace-pre-wrap mt-1">{p.content}</p>
          </li>
        ))}
      </ul>
      <div className="flex justify-center py-4">
        {!isEnd ? (
          <button onClick={() => setSize(size + 1)} disabled={isValidating} className="px-4 py-2 border rounded">
            {isValidating ? "読み込み中..." : "さらに読み込む"}
          </button>
        ) : <span className="text-gray-500">これ以上ありません</span>}
      </div>
    </>
  );
}

import { follow, unfollow } from "../../../lib/api";

function FollowButton({ targetId }: { targetId: string | number }) {
  const [following, setFollowing] = useState<boolean | null>(null); // 実運用では初期状態を取得する
  async function toggle() {
    try {
      if (following) { await unfollow(Number(targetId)); setFollowing(false); }
      else { await follow(Number(targetId)); setFollowing(true); }
    } catch (e: any) {
      alert(e?.message ?? "操作に失敗しました");
    }
  }
  return (
    <button onClick={toggle} className="px-3 py-2 border rounded">
      {following ? "フォロー中" : "フォローする"}
    </button>
  );
}
