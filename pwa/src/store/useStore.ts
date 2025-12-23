import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { User } from '../db/db';

interface AppState {
  user: User | null;
  setUser: (user: User | null) => void;
  logout: () => void;
}

export const useStore = create<AppState>()(
  persist(
    (set) => ({
      user: null,
      setUser: (user) => set({ user }),
      logout: () => set({ user: null }),
    }),
    {
      name: 'aulaviva-storage',
    }
  )
);
