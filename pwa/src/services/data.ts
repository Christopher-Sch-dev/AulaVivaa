import { db, type Subject, type ClassSession } from '../db/db';

export const DataService = {
  // Subjects
  async createSubject(subject: Subject) {
    const id = await db.subjects.add(subject);
    return { ...subject, id };
  },

  async getSubjectsByTeacher(teacherId: number) {
    return await db.subjects.where('teacherId').equals(teacherId).toArray();
  },

  async getAllSubjects() {
    return await db.subjects.toArray();
  },

  async joinSubject(studentId: number, subjectCode: string) {
    const subject = await db.subjects.where('code').equals(subjectCode).first();
    if (!subject) throw new Error('Código de asignatura inválido');

    const existing = await db.enrollments.where({ studentId, subjectId: subject.id! }).first();
    if (existing) throw new Error('Ya estás inscrito en esta asignatura');

    await db.enrollments.add({ studentId, subjectId: subject.id! });
    return subject;
  },

  async getStudentSubjects(studentId: number) {
    const enrollments = await db.enrollments.where('studentId').equals(studentId).toArray();
    const subjectIds = enrollments.map(e => e.subjectId);
    return await db.subjects.where('id').anyOf(subjectIds).toArray();
  },

  // Classes
  async createClass(classSession: ClassSession) {
    const id = await db.classes.add(classSession);
    return { ...classSession, id };
  },

  async getClassesBySubject(subjectId: number) {
    return await db.classes.where('subjectId').equals(subjectId).toArray();
  },

  async getClassById(id: number) {
    return await db.classes.get(id);
  },

  // Stats
  async getTeacherStats(teacherId: number) {
    const subjects = await db.subjects.where('teacherId').equals(teacherId).toArray();
    const subjectIds = subjects.map(s => s.id!);

    const classesCount = await db.classes.where('subjectId').anyOf(subjectIds).count();

    const enrollments = await db.enrollments.where('subjectId').anyOf(subjectIds).toArray();
    const uniqueStudents = new Set(enrollments.map(e => e.studentId)).size;

    return {
      subjects: subjects.length,
      classes: classesCount,
      students: uniqueStudents
    };
  },

  async getStudentStats(studentId: number) {
    const enrollments = await db.enrollments.where('studentId').equals(studentId).toArray();
    const subjectIds = enrollments.map(e => e.subjectId);

    const classesCount = await db.classes.where('subjectId').anyOf(subjectIds).count();

    return {
      subjects: enrollments.length,
      classes: classesCount
    };
  }
};
