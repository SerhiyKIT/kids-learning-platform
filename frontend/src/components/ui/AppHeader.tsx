import { Avatar } from "./Avatar";
import { Badge } from "./Badge";

/** Верхня смуга кабінету дорослого: знак платформи + маркер контексту + користувач. */
export function AppHeader({
  context = "Кабінет дорослого",
  userName,
  onLogout,
}: {
  context?: string;
  userName?: string;
  onLogout?: () => void;
}) {
  return (
    <header className="bg-surface border-line flex items-center justify-between gap-4 border-b px-8 py-4">
      <div className="flex items-center gap-3">
        <span className="bg-brand flex size-[34px] items-center justify-center rounded-[10px]">
          <ShieldCheck />
        </span>
        <span className="text-base font-medium tracking-[-0.005em]">Безпечний Простір</span>
      </div>
      <div className="flex items-center gap-3.5">
        <Badge>{context}</Badge>
        {onLogout ? (
          <button
            type="button"
            onClick={onLogout}
            className="text-ink-muted hover:bg-hover-soft hover:text-ink cursor-pointer rounded-lg px-2.5 py-1.5 text-sm"
          >
            Вийти
          </button>
        ) : null}
        {userName ? <Avatar name={userName} /> : null}
      </div>
    </header>
  );
}

function ShieldCheck() {
  return (
    <svg width="19" height="19" viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <path
        d="M10 2.2 3.9 4.5v5.1c0 3.6 2.5 6.6 6.1 8.2 3.6-1.6 6.1-4.6 6.1-8.2V4.5L10 2.2Z"
        stroke="#ffffff"
        strokeWidth="1.5"
        strokeLinejoin="round"
      />
      <path
        d="M7.2 9.9l2.1 2.1 3.6-3.9"
        stroke="#ffffff"
        strokeWidth="1.6"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}
