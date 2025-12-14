export const getThemeStyles = (isDark: boolean) => ({
  // Фон сторінки
  bg: isDark ? '#0a0b1e' : '#f4f6f8',

  // Основний колір тексту (виправлено на білий для темної теми)
  text: isDark ? '#ffffff' : '#1a1a1a',

  // Колір тексту для описів (трохи менш яскравий, але читабельний)
  textSecondary: isDark ? '#e0e0e0' : '#555555',

  primary: isDark ? '#00e5ff' : '#1565c0', // Неон блакитний / Класичний синій
  secondary: isDark ? '#d500f9' : '#5e35b1', // Неон фіолетовий

  // Фон карток (зробимо трохи світлішим за фон сторінки для контрасту в темній темі)
  cardBg: isDark ? 'rgba(30, 32, 60, 0.95)' : '#ffffff',

  cardBorder: isDark ? '1px solid rgba(0, 229, 255, 0.3)' : '1px solid #e0e0e0',
  glow: isDark ? '0 0 20px rgba(0, 229, 255, 0.4)' : 'none',

  isDark, // повертаємо сам прапорець
});

export type ThemeStyles = ReturnType<typeof getThemeStyles>;
