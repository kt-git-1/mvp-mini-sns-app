import { cookies } from "next/headers";
export const runtime = "nodejs";

const API = process.env.NEXT_PUBLIC_API_BASE_URL!;
const COOKIE = process.env.SESSION_COOKIE_NAME ?? "ms_token";

export async function GET(req: Request, { params }: { params: { id: string } }) {
  const token = (await cookies()).get(COOKIE)?.value;
  if (!token) return Response.json({ error: "unauthorized" }, { status: 401 });

  const { searchParams } = new URL(req.url);
  const size = searchParams.get("size") ?? "20";
  const cursor = searchParams.get("cursor") ?? "";

  const upstream = new URL(`${API}/users/${params.id}/posts`);
  upstream.searchParams.set("size", size);
  if (cursor) upstream.searchParams.set("cursor", cursor);

  const r = await fetch(upstream, {
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });
  return new Response(await r.text(), {
    status: r.status,
    headers: { "Content-Type": r.headers.get("Content-Type") ?? "application/json" },
  });
}
