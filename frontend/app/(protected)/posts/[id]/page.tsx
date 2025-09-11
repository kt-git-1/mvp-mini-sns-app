import { getPost } from "../../../lib/api";
import Link from "next/link";

// サーバコンポーネント
export default async function PostDetail({ params }: { params: { id: string } }) {
  const post = await getPost(params.id);

  return (
    <main className="max-w-xl mx-auto p-4 space-y-4">
      <div className="text-sm text-gray-600">
        <Link href={`/users/${post.user?.id ?? ""}`} className="underline">
          {post.user?.username ?? "unknown"}
        </Link>{" "}
        ・ {post.createdAt ? new Date(post.createdAt).toLocaleString() : ""}
      </div>
      <p className="whitespace-pre-wrap text-lg">{post.content}</p>
      <div className="flex gap-3">
        {/* TODO: 自分の投稿かどうかの判定はサーバ側の me と突き合わせる */}
        <DeletePostButton id={params.id} />
        <Link href="/" className="px-3 py-2 border rounded">戻る</Link>
      </div>
    </main>
  );
}

// クライアント側の削除ボタン
"use client";
import { useRouter } from "next/navigation";
import { deletePost } from "../../../lib/api";

function DeletePostButton({ id }: { id: string }) {
  const r = useRouter();
  async function onDelete() {
    if (!confirm("この投稿を削除しますか？")) return;
    try {
      await deletePost(id);
      r.replace("/"); // タイムラインへ
      r.refresh();
    } catch (e: any) {
      alert(e?.message ?? "削除に失敗しました");
    }
  }
  return <button onClick={onDelete} className="px-3 py-2 border rounded">削除</button>;
}
