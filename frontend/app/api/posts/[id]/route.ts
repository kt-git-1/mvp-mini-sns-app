import { cookies } from "next/headers";
export const runtime = "nodejs";

const API = process.env.NEXT_PUBLIC_API_BASE_URL!;
const COOKIE = process.env.SESSION_COOKIE_NAME ?? "ms_token";

async function getToken() {
  const jar = await cookies();
  return jar.get(COOKIE)?.value ?? null;
}

export async function GET(_req: Request, ctx: { params: { id: string } }) {
  const token = await getToken();
  if (!token) return Response.json({ error: "unauthorized" }, { status: 401 });

  const r = await fetch(`${API}/posts/${ctx.params.id}`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });
  return new Response(await r.text(), {
    status: r.status,
    headers: { "Content-Type": r.headers.get("Content-Type") ?? "application/json" },
  });
}

export async function DELETE(_req: Request, ctx: { params: { id: string } }) {
  const token = await getToken();
  if (!token) return Response.json({ error: "unauthorized" }, { status: 401 });

  const r = await fetch(`${API}/posts/${ctx.params.id}`, {
    method: "DELETE",
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });
  return new Response(null, { status: r.status }); // 204 期待
}
