import { useState, useEffect } from 'react';

function useLocalStorage<T>(key: string, initialValue: T) {
  // 1. Отримуємо значення з localStorage або використовуємо початкове
  const [storedValue, setStoredValue] = useState<T>(() => {
    try {
      if (typeof window === 'undefined') {
        return initialValue;
      }
      const item = window.localStorage.getItem(key);
      return item ? JSON.parse(item) : initialValue;
    } catch (error) {
      console.warn(`Error reading localStorage key “${key}”:`, error);
      return initialValue;
    }
  });

  // 2. Функція для оновлення стану та localStorage
  const setValue = (value: T | ((val: T) => T)) => {
    try {
      // Дозволяє передавати функцію, як у звичайному useState
      const valueToStore = value instanceof Function ? value(storedValue) : value;

      setStoredValue(valueToStore);

      if (typeof window !== 'undefined') {
        window.localStorage.setItem(key, JSON.stringify(valueToStore));
      }
    } catch (error) {
      console.warn(`Error setting localStorage key “${key}”:`, error);
    }
  };

  return [storedValue, setValue] as const;
}

export default useLocalStorage;
