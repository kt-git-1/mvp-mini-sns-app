"use client";
import { useRouter, useSearchParams } from "next/navigation";
import { signup } from "../../lib/api";

function resolveNext(sp: URLSearchParams) {
  const fallback = "/mypage";
  const n = sp.get("next");
  if (!n) return fallback;
  try {
    const u = new URL(n, window.location.origin);
    if (
      u.origin !== window.location.origin ||
      u.pathname.startsWith("/api") ||
      u.pathname.startsWith("/_next") ||
      u.pathname === "/login" ||
      u.pathname === "/signup"
    ) return fallback;
    return `${u.pathname}${u.search}${u.hash}`;
  } catch { return fallback; }
}

export default function Page() {
  const r = useRouter();
  const sp = useSearchParams();

  async function onSubmit(formData: FormData) {
    const username = String(formData.get("username") || "");
    const password = String(formData.get("password") || "");
    // バリデーション
    if (username.length < 3 || username.length > 30 || password.length < 8 || password.length > 128) {
      alert("入力の長さが不正です");
      return;
    }

    try {
      await signup(username, password);        // ← BFF が signup + auto login + Cookie 設定
      alert("サインアップしました");
      r.replace(resolveNext(sp));              // ← デフォルト /mypage
    } catch (e: any) {
      alert(e?.message ?? "サインアップに失敗しました");
    }
  }

  return (
    <form action={onSubmit} className="max-w-sm mx-auto p-6 space-y-3">
      <input name="username" placeholder="3〜30文字" className="border p-2 w-full" />
      <input name="password" type="password" placeholder="8〜128文字" className="border p-2 w-full" />
      <button className="bg-black text-white px-4 py-2">Create account</button>
    </form>
  );
}
