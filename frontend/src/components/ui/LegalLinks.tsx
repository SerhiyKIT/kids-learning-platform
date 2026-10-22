import Link from "next/link";

/** The public pages' footer: the two legal drafts, always reachable before an account exists. */
export function LegalLinks() {
  return (
    <nav aria-label="Правова інформація" className="text-ink-faint flex flex-wrap justify-center gap-x-4 gap-y-1 text-[13px]">
      <Link href="/terms">Умови використання</Link>
      <Link href="/privacy">Політика конфіденційності</Link>
    </nav>
  );
}
