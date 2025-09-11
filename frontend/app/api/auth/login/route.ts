import { cookies } from "next/headers";
export const runtime = "nodejs";

const API = process.env.NEXT_PUBLIC_API_BASE_URL!;
const COOKIE = process.env.SESSION_COOKIE_NAME ?? "ms_token";
const isProd = () => process.env.NODE_ENV === "production";

export async function POST(req: Request) {
  try {
    const { username, password } = await req.json();
    if (typeof username !== "string" || typeof password !== "string") {
      return Response.json({ error: "invalid body" }, { status: 400 });
    }
    if (username.length < 3 || username.length > 30 || password.length < 8 || password.length > 128) {
      return Response.json({ error: "validation error" }, { status: 400 });
    }

    // 1) Springでログイン
    const r = await fetch(`${API}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password }),
      cache: "no-store",
    });
    if (!r.ok) return Response.json({ error: "auto login failed" }, { status: 502 });

    const { token } = await r.json();

    // 3) サーバ側で httpOnly Cookie をセット
    const jar = await cookies();
    jar.set(COOKIE, token, {
      httpOnly: true,
      sameSite: "lax",
      path: "/",
      maxAge: 60 * 60 * 24,  // 24h
      secure: isProd(),
    });

    // ※ token は返さない
    return Response.json({ authenticated: true }, { status: 201 });
  } catch {
    return Response.json({ error: "bad request" }, { status: 400 });
  }
}
