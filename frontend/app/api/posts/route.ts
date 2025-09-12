import { cookies } from "next/headers";
export const runtime = "nodejs";

const API = process.env.NEXT_PUBLIC_API_BASE_URL!;
const COOKIE = process.env.SESSION_COOKIE_NAME!;

/** 新規投稿エンドポイント。認証必須 */
export async function POST(req: Request) {
  try {
    // ① クッキーからJWTを取り出す（無ければ401）
    const jar = await cookies();
    const token = jar.get(COOKIE)?.value;
    if (!token) {
      return Response.json({ error: "unauthorized" }, { status: 401 });
    }

    // ② ボディをJSONとして読み取り、contentをバリデーション
    let body: any;
    try {
      body = await req.json();
    } catch {
      return Response.json({ error: "invalid body" }, { status: 400 });
    }

    const content =
      typeof body?.content === "string" ? body.content.trim() : "";
    if (content.length === 0 || content.length > 280) {
      return Response.json(
        { error: "content must be 1–280 chars" },
        { status: 400 }
      );
    }

    // ③ Springの /posts にプロキシ
    let res: Response;
    try {
      res = await fetch(`${API}/posts`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ content }),
        cache: "no-store",
      });
      console.log(res)
    } catch (err) {
      console.error("Fetch error:", err);
      return Response.json(
        { error: "failed to reach backend" },
        { status: 502 }
      );
    }

    // ④ ステータスと本文をそのまま返す（201 期待）
    const text = await res.text();
    return new Response(text, {
      status: res.status,
      headers: {
        "Content-Type":
          res.headers.get("Content-Type") ?? "application/json",
      },
    });
  } catch (err) {
    // 予期せぬ例外をキャッチ
    console.error("Unexpected error:", err);
    return Response.json(
      { error: "internal server error" },
      { status: 500 }
    );
  }
}
