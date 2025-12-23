import { db, User } from '../db/db';
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
                const pdfUrl = 'https://desarrollodocente.uc.cl/wp-content/uploads/2020/09/Una-breve-mirada-al-estado-actual-de-la-Inteligencia-Artificial.pdf';

                try {
                    const response = await fetch(pdfUrl, { mode: 'cors' });
                    if (!response.ok) throw new Error('Fetch failed');

                    const blob = await response.blob();

                    await DataService.createClass({
                        name: 'Estado Actual de la IA',
                        description: 'Lectura obligatoria sobre el impacto de la IA en la educación.',
                        date: new Date().toISOString(),
                        subjectId: subject.id!,
                        pdfFile: blob,
                        pdfName: 'IA_Estado_Actual.pdf'
                    });
                    console.log('✅ Seeded Class & PDF');

                } catch (e) {
                    console.warn('⚠️ PDF Fetch failed (CORS?):', e);
                    const dummyBlob = new Blob(['Dummy PDF Content for Demo'], { type: 'application/pdf' });
                    await DataService.createClass({
                        name: 'Estado Actual de la IA (Demo)',
                        description: 'No se pudo descargar el PDF real por CORS. Este es un archivo de prueba. (Detalle: ' + (e as any).message + ')',
                        date: new Date().toISOString(),
                        subjectId: subject.id!,
                        pdfFile: dummyBlob,
                        pdfName: 'Demo_Fallback.pdf'
                    });
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
