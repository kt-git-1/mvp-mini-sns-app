import { cookies } from "next/headers";
import { mypage } from "../../lib/api";

export default async function Page() {
  const cookieStore = await cookies();
  const token = cookieStore.get(process.env.SESSION_COOKIE_NAME!)?.value!;

  try {
    const data = await mypage(); // { username }
    return <div className="max-w-xl mx-auto p-6">Hello, <b>{data.username}</b></div>;
  } catch (e: any) {
    return <div className="max-w-xl mx-auto p-6">エラーが発生しました: {e?.message}</div>;
  }
}