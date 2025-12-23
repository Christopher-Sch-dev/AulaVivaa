import { db, type User } from '../db/db';
import { DataService } from './data';

export const SeedService = {
    async seedDemoData() {
        try {
            console.log('🌱 Verifying Demo Data Integrity...');

            // 1. Ensure Teacher Exists
            let teacher = await db.users.where('email').equals('d1@d1.cl').first();
            if (!teacher) {
                const id = await db.users.add({
                    name: 'Docente Demo',
                    email: 'd1@d1.cl',
                    passwordHash: 'docente12',
                    role: 'docente'
                });
                teacher = { id, name: 'Docente Demo', email: 'd1@d1.cl', role: 'docente' } as User;
                console.log('✅ Teacher Created');
            }

            // 2. Ensure Student Exists
            let student = await db.users.where('email').equals('a1@a1.cl').first();
            if (!student) {
                const id = await db.users.add({
                    name: 'Alumno Demo',
                    email: 'a1@a1.cl',
                    passwordHash: 'alumno12',
                    role: 'alumno'
                });
                student = { id, name: 'Alumno Demo', email: 'a1@a1.cl', role: 'alumno' } as User;
                console.log('✅ Student Created');
            }

            // 3. Ensure Subject Exists
            const subjectCode = 'IA-2025';
            let subject = await db.subjects.where('code').equals(subjectCode).first();
            if (!subject) {
                subject = await DataService.createSubject({
                    code: subjectCode,
                    name: 'Inteligencia Artificial',
                    description: 'Fundamentos y Estado del Arte de la IA Generativa',
                    teacherId: teacher.id!
                });
                console.log('✅ Subject Created');
            } else {
                // Ensure subject belongs to demo teacher (fix if ID changed due to re-seed)
                if (subject.teacherId !== teacher.id) {
                    await db.subjects.update(subject.id!, { teacherId: teacher.id });
                    subject.teacherId = teacher.id!; // local update
                    console.log('🔧 Subject Ownership Corrected');
                }
            }

            // 4. Ensure Class Exists (Atomic Check)
            // We look for a class in this subject. If none, we create the demo class.
            const existingClass = await db.classes.where('subjectId').equals(subject.id!).first();

            if (!existingClass) {
                console.log('⚠️ Class missing for Subject, creating...');

                // Robust Base64 PDF (Welcome to Aula Viva) - SHORT VERSION "Hello World" PDF to avoid huge file size issues in chat
                // In a real scenario, this would be the longer Blob.
                // This is a minimal valid PDF showing "Bienvenido a Aula Viva".
                const pdfBase64 = "JVBERi0xLjcKMSAwIG9iago8PC9UeXBlL0NhdGFsb2cvUGFnZXMgMiAwIFI+PgplbmRvYmoyIDAgb2JqCjw8L1R5cGUvUGFnZXMvQ291bnQgMS9LaWRzWzMgMCBSXT4+CmVuZG9iajMgMCBvYmoKPDwvVHlwZS9QYWdlL01lZGlhQm94WzAgMCA1OTUgODQyXS9QYXJlbnQgMiAwIFIvUmVzb3VyY2VzPDwvRm9udDw8L0YxIDQgMCBSPj4+Pi9Db250ZW50cyA1IDAgUj4+CmVuZG9iajQgMCBvYmoKPDwvVHlwZS9Gb250L1N1YnR5cGUvVHlwZTEvQmFzZUZvbnQvSGVsdmV0aWNhPj4KZW5kb2JqNSAwIG9iago8PC9MZW5ndGggNDQ+PgpzdHJlYW0KQlQKL0YxIDI0IFRmCjEwMCA3MDAgVGQKKEJpZW52ZW5pZG8gYSBBdWxhIFZpdmEgSW50ZWxpZ2VuY2lhIEFydGlmaWNpYWwpIFRqCkVUCmVuZHN0cmVhbQplbmRvYmoKeHJlZgowIDYKMDAwMDAwMDAwMCA2NTUzNSBmIAowMDAwMDAwMDEwIDAwMDAwIG4gCjAwMDAwMDAwNjAgMDAwMDAgbiAKMDAwMDAwMDExNyAwMDAwMCBuIAowMDAwMDAwMjQ1IDAwMDAwIG4gCjAwMDAwMDAzMzMgMDAwMDAgbiAKdHJhaWxlcgo8PC9TaXplIDYvUm9vdCAxIDAgUj4+CnN0YXJ0eHJlZgozOTgKJSVFT0YK";

                const base64ToBlob = (base64: string, type: string) => {
                    const binStr = atob(base64);
                    const len = binStr.length;
                    const arr = new Uint8Array(len);
                    for (let i = 0; i < len; i++) {
                        arr[i] = binStr.charCodeAt(i);
                    }
                    return new Blob([arr], { type: type });
                };

                const blob = base64ToBlob(pdfBase64, 'application/pdf');

                await DataService.createClass({
                    name: 'Estado Actual de la IA (Demo)',
                    description: 'Documento PDF Demo generado localmente (Offline).',
                    date: new Date().toISOString(),
                    subjectId: subject.id!,
                    pdfFile: blob,
                    pdfName: 'Demo_IA.pdf'
                });
                console.log('✅ Class & PDF Created (Embedded)');
            }

            // 5. Ensure Enrollment Exists
            const enrollment = await db.enrollments.where({ studentId: student.id!, subjectId: subject.id! }).first();
            if (!enrollment) {
                await DataService.joinSubject(student.id!, subjectCode);
                console.log('✅ Enrollment Created');
            }

        } catch (error) {
            console.error('❌ Seeding Fatal Error:', error);
        }
    }
};
