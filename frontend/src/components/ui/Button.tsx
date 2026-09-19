/** Основна дія: повна ширина, суцільний бренд-фон. */
export function Button({
  children,
  className = "",
  ...props
}: React.ComponentPropsWithoutRef<"button">) {
  return (
    <button
      {...props}
      className={`bg-brand hover:bg-brand-hover active:bg-brand-active h-12 w-full cursor-pointer rounded-lg text-base font-medium text-white transition-colors duration-100 disabled:cursor-default disabled:opacity-50 ${className}`}
    >
      {children}
    </button>
  );
}
