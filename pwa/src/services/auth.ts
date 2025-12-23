import { db, type User } from '../db/db';

export const AuthService = {
  async register(user: Omit<User, 'id'>) {
    const existing = await db.users.where('email').equals(user.email).first();
    if (existing) {
      throw new Error('El usuario ya existe');
    }
    const id = await db.users.add(user);
    return { ...user, id };
  },

  async login(email: string, password: string) {
    // --- DEMO USERS (Bypass DB) ---
    if (email === 'd1@d1.cl' && password === 'docente12') {
      return { id: 9991, name: 'Juan Pérez (Demo Docente)', email, passwordHash: 'docente12', role: 'docente' as const };
    }
    if (email === 'a1@a1.cl' && password === 'alumno12') {
      return { id: 9992, name: 'María González (Demo Alumno)', email, passwordHash: 'alumno12', role: 'alumno' as const };
    }

    const user = await db.users.where('email').equals(email).first();
    if (!user || user.passwordHash !== password) {
      throw new Error('Credenciales inválidas');
    }
    return user;
  },

  async getCurrentUser(id: number) {
    return await db.users.get(id);
  }
};
