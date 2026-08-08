// Real backgrounds/characters/icons don't exist yet — this is the ONLY place that knows that.
// Swapping in real art later means teaching this component to resolve `id` to a real asset,
// without touching any scene-rendering logic.

const PALETTE: Record<Kind, string> = {
  background: "bg-sky-200 border-sky-400 text-sky-900",
  character: "bg-amber-200 border-amber-400 text-amber-900",
  icon: "bg-emerald-200 border-emerald-400 text-emerald-900",
};

type Kind = "background" | "character" | "icon";

export function SceneAsset({ kind, id, className = "" }: { kind: Kind; id: string; className?: string }) {
  return (
    <div
      className={`flex items-center justify-center rounded-2xl border-4 border-dashed ${PALETTE[kind]} ${className}`}
      role="img"
      aria-label={`${kind}: ${id}`}
    >
      <span className="px-2 text-center text-sm leading-tight font-semibold break-words">{id}</span>
    </div>
  );
}
