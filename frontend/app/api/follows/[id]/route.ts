import { cookies } from "next/headers";
export const runtime = "nodejs";

const API = process.env.NEXT_PUBLIC_API_BASE_URL!;
const COOKIE = process.env.SESSION_COOKIE_NAME!;

// 認証チェックを共通化するヘルパー
async function getToken() {
  const jar = await cookies();
  return jar.get(COOKIE)?.value ?? null;
}

/** フォロー登録 */
export async function POST(_req: Request, ctx: { params: { id: string } }) {
  const token = await getToken();
  if (!token) return Response.json({ error: "unauthorized" }, { status: 401 });

  const res = await fetch(`${API}/follows/${ctx.params.id}`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });
  return new Response(null, { status: res.status }); // 204が期待値
}

/** フォロー解除 */
export async function DELETE(_req: Request, ctx: { params: { id: string } }) {
  const token = await getToken();
  if (!token) return Response.json({ error: "unauthorized" }, { status: 401 });

  const res = await fetch(`${API}/follows/${ctx.params.id}`, {
    method: "DELETE",
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });
  return new Response(null, { status: res.status }); // 204が期待値
}
