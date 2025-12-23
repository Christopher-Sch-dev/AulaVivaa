import Dexie, { type EntityTable } from 'dexie';

// Define Interfaces matching our plan
export interface User {
  id?: number; // Auto-incremented
  email: string;
  passwordHash: string; // Stored simply for demo
  name: string;
  role: 'docente' | 'alumno';
}

export interface Subject {
  id?: number;
  name: string;
  description: string;
  code: string; // Unique join code
  teacherId: number;
}

export interface ClassSession {
  id?: number;
  name: string;
  description: string;
  date: string;
  pdfFile: Blob; // The actual PDF file
  pdfName: string;
  subjectId: number;
}

export interface Enrollment {
  id?: number;
  studentId: number;
  subjectId: number;
}

export interface ChatMessage {
  id?: number;
  classId: number;
  sender: 'user' | 'ai';
  text: string;
  timestamp: number;
}

// Database Declaration
const db = new Dexie('AulaVivaDB') as Dexie & {
  users: EntityTable<User, 'id'>;
  subjects: EntityTable<Subject, 'id'>;
  classes: EntityTable<ClassSession, 'id'>;
  enrollments: EntityTable<Enrollment, 'id'>;
  messages: EntityTable<ChatMessage, 'id'>;
};

// Schema Definition
db.version(1).stores({
  users: '++id, &email',
  subjects: '++id, &code, teacherId',
  classes: '++id, subjectId',
  enrollments: '++id, [studentId+subjectId], studentId, subjectId',
  messages: '++id, classId'
});

export { db };
