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
