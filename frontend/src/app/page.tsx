import Link from "next/link";

export default function Home() {
  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-6 p-8 text-center">
      <h1 className="text-2xl font-semibold">Kids Learning Platform</h1>
      <div className="flex gap-4">
        <Link href="/login" className="rounded-full bg-foreground px-5 py-2 text-background">
          Log in
        </Link>
        <Link href="/register" className="rounded-full border border-black/[.12] px-5 py-2 dark:border-white/[.16]">
          Register
        </Link>
      </div>
    </main>
  );
}
