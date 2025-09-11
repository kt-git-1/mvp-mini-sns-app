import { NextResponse, type NextRequest } from "next/server";

const COOKIE_NAME = process.env.SESSION_COOKIE_NAME ?? "ms_token";

/** 公開パス（未認証でも通す） */
const PUBLIC_PATHS = [
  "/login",
  "/signup",
  "/api/session",   // セッション操作
  "/api/auth",      // ← 追加：認証系BFF (/api/auth/signup, /api/auth/login など)
  "/favicon",
  "/favicon.ico",
  "/robots.txt",
  "/sitemap.xml",
  "/manifest.webmanifest",
  "/_next",         // Next内部
] as const;

function isPublicPath(pathname: string) {
  return PUBLIC_PATHS.some((p) => pathname === p || pathname.startsWith(p + "/"));
}
function isApiPath(pathname: string) {
  return pathname.startsWith("/api");
}
function wantsHTML(req: NextRequest) {
  const accept = req.headers.get("accept") ?? "";
  const dest = req.headers.get("sec-fetch-dest") ?? "";
  return accept.includes("text/html") || dest === "document";
}

/** オープンリダイレクト対策：同一オリジンかつ安全なパスだけ許可 */
function sanitizeNext(next: string | null, req: NextRequest, fallback = "/mypage") {
  if (!next) return fallback;
  try {
    const u = new URL(next, req.nextUrl.origin);
    if (
      u.origin !== req.nextUrl.origin ||
      u.pathname.startsWith("/api") ||
      u.pathname.startsWith("/_next") ||
      u.pathname === "/login" ||
      u.pathname === "/signup"
    ) return fallback;
    return `${u.pathname}${u.search}${u.hash}`;
  } catch {
    return fallback;
  }
}

/** CORS プリフライト（必要に応じて調整） */
function preflightOk(req: NextRequest) {
  const origin = req.headers.get("origin") ?? "*";
  const res = new NextResponse(null, { status: 204 });
  res.headers.set("Access-Control-Allow-Origin", origin);
  res.headers.set("Vary", "Origin");
  res.headers.set("Access-Control-Allow-Credentials", "true");
  res.headers.set("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
  res.headers.set(
    "Access-Control-Allow-Headers",
    req.headers.get("access-control-request-headers") ?? "content-type,authorization"
  );
  return res;
}

export function middleware(req: NextRequest) {
  const { pathname, search } = req.nextUrl;

  // 1) 公開パスは素通り。ただし認証済みで /login|/signup を開いたら迂回
  if (isPublicPath(pathname)) {
    const isAuth = !!req.cookies.get(COOKIE_NAME)?.value;
    if (isAuth && (pathname === "/login" || pathname === "/signup")) {
      const safe = sanitizeNext(req.nextUrl.searchParams.get("next"), req);
      return NextResponse.redirect(new URL(safe, req.url));
    }
    return NextResponse.next();
  }

  // 2) 認証チェック（httpOnly Cookie 前提）
  const token = req.cookies.get(COOKIE_NAME)?.value;
  if (token) return NextResponse.next();

  // --- 未認証時の分岐 ---
  // 3) API は JSON 401（プリフライトは 204）
  if (isApiPath(pathname)) {
    if (req.method === "OPTIONS") return preflightOk(req);
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }

  // 4) HTML ナビゲーションは /login へ（復帰先にクエリも含めて保持）
  if (wantsHTML(req)) {
    const url = new URL("/login", req.url);
    url.searchParams.set("next", pathname + (search || ""));
    return NextResponse.redirect(url);
  }

  // 5) それ以外は JSON 401
  return NextResponse.json({ error: "unauthorized" }, { status: 401 });
}

/** ミドルウェア適用範囲（静的系は除外） */
export const config = {
  matcher: [
    "/((?!_next/static|_next/image|favicon.ico|robots.txt|sitemap.xml|manifest.webmanifest).*)",
  ],
};
