"use client";
import { useRouter, useSearchParams } from "next/navigation";
import { login } from "../../lib/api";

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
    const username = String(formData.get("username")||"");
    const password = String(formData.get("password")||"");
    // バリデーション
    if (username.length < 3 || password.length < 8) return alert("入力が短すぎます");
    if (username.length > 30 || password.length > 128) return alert("入力が長すぎます");

    try {
        await login(username, password);
        alert("ログインしました");
        r.replace(resolveNext(sp)); 
      } catch (e: any) {
        alert(e?.message ?? "ログインに失敗しました");
      }
  }

  return (
    <form action={onSubmit} className="max-w-sm mx-auto p-6 space-y-3">
      <input name="username" className="border p-2 w-full" placeholder="username" />
      <input name="password" type="password" className="border p-2 w-full" placeholder="password" />
      <button className="bg-black text-white px-4 py-2">Login</button>
    </form>
  );
}
