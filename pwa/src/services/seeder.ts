import { db, type User } from '../db/db';
import { DataService } from './data';

export const SeedService = {
    async seedDemoData() {
        try {
            console.log('🌱 Starting Data Seeding...');

            // 1. Create Demo Teacher
            let teacher = await db.users.where('email').equals('d1@d1.cl').first();
            if (!teacher) {
                const id = await db.users.add({
                    name: 'Docente Demo',
                    email: 'd1@d1.cl',
                    passwordHash: 'docente12',
                    role: 'docente'
                });
                teacher = { id, name: 'Docente Demo', email: 'd1@d1.cl', role: 'docente' } as User;
                console.log('✅ Seeded Teacher');
            }

            // 2. Create Demo Student
            let student = await db.users.where('email').equals('a1@a1.cl').first();
            if (!student) {
                const id = await db.users.add({
                    name: 'Alumno Demo',
                    email: 'a1@a1.cl',
                    passwordHash: 'alumno12',
                    role: 'alumno'
                });
                student = { id, name: 'Alumno Demo', email: 'a1@a1.cl', role: 'alumno' } as User;
                console.log('✅ Seeded Student');
            }

            // 3. Check/Create Subject
            const subjectCode = 'IA-2025';
            let subject = await db.subjects.where('code').equals(subjectCode).first();

            if (!subject) {
                subject = await DataService.createSubject({
                    code: subjectCode,
                    name: 'Inteligencia Artificial',
                    description: 'Fundamentos y Estado del Arte de la IA Generativa',
                    teacherId: teacher.id!
                });
                console.log('✅ Seeded Subject');

                // 4. Create Class with PDF

                // Minimal Valid PDF (Hello World) Base64 to guarantee offline functionality
                // This avoids CORS issues completely.
                const base64PDF = "JVBERi0xLjcKCjEgMCBvYmogICUgZW50cnkgcG9pbnQKPDwKICAvVHlwZSAvQ2F0YWxvZwogIC9QYWdlcyAyIDAgUgo+PgplbmRvYmoKCjIgMCBvYmoKPDwKICAvVHlwZSAvUGFnZXMKICAvTWVkaWFCb3ggWyAwIDAgNTk1LjI4IDg0MS44OSBdCiAgL0NvdW50IDEKICAvS2lkcyBbIDMgMCBSIF0KPj4KZW5kb2JqCgozIDAgb2JqCjw8CiAgL1R5cGUgL1BhZ2UKICAvUGFyZW50IDIgMCBSCiAgL1Jlc291cmNlcyA8PAogICAgL0ZvbnQgPDwKICAgICAgL0YxIDQgMCBSC    
                // Truncated for brevity manually, let's use a real full string or fetch a reliable data URI?
                // Actually, let's try to fetch a DATA URI if possible, or construct Blob from Base64.
                // It's safer to use a function to convert base64.

                // I'll use a reliable, simple PDF content here.
                // Title: "Aula Viva Demo PDF"
                const pdfBase64 = "JVBERi0xLjcKMSAwIG9iago8PC9UeXBlL0NhdGFsb2cvUGFnZXMgMiAwIFI+PgplbmRvYmoyIDAgb2JqCjw8L1R5cGUvUGFnZXMvQ291bnQgMS9LaWRzWzMgMCBSXT4+CmVuZG9iajMgMCBvYmoKPDwvVHlwZS9QYWdlL01lZGlhQm94WzAgMCA1OTUgODQyXS9QYXJlbnQgMiAwIFIvUmVzb3VyY2VzPDwvRm9udDw8L0YxIDQgMCBSPj4+Pi9Db250ZW50cyA1IDAgUj4+CmVuZG9iajQgMCBvYmoKPDwvVHlwZS9Gb250L1N1YnR5cGUvVHlwZTEvQmFzZUZvbnQvSGVsdmV0aWNhPj4KZW5kb2JqNSAwIG9iago8PC9MZW5ndGggNDQ+PgpzdHJlYW0KQlQKL0YxIDI0IFRmCjEwMCA3MDAgVGQKKEJpZW52ZW5pZG8gYSBBdWxhIFZpdmEgSW50ZWxpZ2VuY2lhIEFydGlmaWNpYWwpIFRqCkVUCmVuZHN0cmVhbQplbmRvYmoKeHJlZgowIDYKMDAwMDAwMDAwMCA2NTUzNSBmIAowMDAwMDAwMDEwIDAwMDAwIG4gCjAwMDAwMDAwNjAgMDAwMDAgbiAKMDAwMDAwMDExNyAwMDAwMCBuIAowMDAwMDAwMjQ1IDAwMDAwIG4gCjAwMDAwMDAzMzMgMDAwMDAgbiAKdHJhaWxlcgo8PC9TaXplIDYvUm9vdCAxIDAgUj4+CnN0YXJ0eHJlZgozOTgKJSVFT0YK";

                // Helper to convert Base64 to Blob
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

                try {
                    await DataService.createClass({
                        name: 'Estado Actual de la IA (Demo)',
                        description: 'Documento PDF Demo generado localmente (Offline). Linkea a Docente ID: ' + teacher.id,
                        date: new Date().toISOString(),
                        subjectId: subject.id!,
                        pdfFile: blob,
                        pdfName: 'Demo_IA.pdf'
                    });
                    console.log('✅ Seeded Class & PDF (Embedded)');

                } catch (e) {
                    console.error('Failed to seed class:', e);
                }
            }

            // 5. Enroll Student
            const enrollment = await db.enrollments.where({ studentId: student.id!, subjectId: subject.id! }).first();
            if (!enrollment) {
                await DataService.joinSubject(student.id!, subjectCode);
                console.log('✅ Enrolled Demo Student');
            }

        } catch (error) {
            console.error('❌ Seeding Error:', error);
        }
    }
};
