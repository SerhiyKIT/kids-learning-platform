/** Поле одноразового коду: великий моноширинний шрифт, по центру. Решту атрибутів задає виклик. */
export function CodeInput({
  invalid,
  className = "",
  ...props
}: React.ComponentPropsWithoutRef<"input"> & { invalid?: boolean }) {
  return (
    <input
      {...props}
      aria-invalid={invalid || undefined}
      spellCheck={false}
      className={`bg-surface text-ink h-15 w-full rounded-lg border px-3.5 text-center font-mono text-[28px] tracking-[0.3em] ${
        invalid ? "border-danger-field" : "border-line-field"
      } ${className}`}
    />
  );
}
