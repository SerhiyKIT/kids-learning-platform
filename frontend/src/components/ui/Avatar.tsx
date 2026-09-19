/** Кружок з першою літерою імені. Розмір 34 (шапка) або 52 (карточка). */
export function Avatar({
  name,
  size = 34,
  tint,
}: {
  name: string;
  size?: 34 | 52;
  tint?: string;
}) {
  const initial = name.trim().charAt(0).toUpperCase() || "?";
  const big = size === 52;
  return (
    <span
      aria-hidden="true"
      style={tint ? { background: tint } : undefined}
      className={`border-line-chip text-avatar-ink flex flex-none items-center justify-center rounded-full border font-medium ${
        big ? "size-13 text-xl" : "size-[34px] text-sm"
      } ${tint ? "" : "bg-avatar-surface"}`}
    >
      {initial}
    </span>
  );
}

/** Стабільний ненасичений відтінок аватара з імені — щоб картки не були однаковими. */
const AVATAR_TINTS = ["#e8eef2", "#eceaf4", "#e9f1ec", "#f2ece8", "#e8eff4"];

export function tintFor(seed: string): string {
  let hash = 0;
  for (let i = 0; i < seed.length; i += 1) hash = (hash * 31 + seed.charCodeAt(i)) % 9973;
  return AVATAR_TINTS[hash % AVATAR_TINTS.length];
}
