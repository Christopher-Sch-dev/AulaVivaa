import { db, type User } from '../db/db';
import { DataService } from './data';

export const SeedService = {
    async seedDemoData() {
        try {
            console.log('🌱 Verifying Demo Data Integrity (Local Mode)...');

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
                if (subject.teacherId !== teacher.id) {
                    await db.subjects.update(subject.id!, { teacherId: teacher.id });
                    subject.teacherId = teacher.id!;
                    console.log('🔧 Subject Ownership Corrected');
                }
            }

            // 4. Ensure Class Exists (Force Update with Local PDF)
            const existingClass = await db.classes.where('subjectId').equals(subject.id!).first();

            try {
                // Fetch from LOCAL public folder
                const response = await fetch('/demo_ai.pdf');
                if (!response.ok) throw new Error('Local PDF not found in /public/demo_ai.pdf');

                const blob = await response.blob();
                const pdfName = 'IA_Estado_Actual.pdf';

                if (!existingClass) {
                    await DataService.createClass({
                        name: 'Estado Actual de la IA (Demo)',
                        description: 'Lectura obligatoria sobre el impacto de la IA en la educación.',
                        date: new Date().toISOString(),
                        subjectId: subject.id!,
                        pdfFile: blob,
                        pdfName: pdfName
                    });
                    console.log('✅ Class & PDF Created (From Local Public)');
                } else {
                    // FORCE UPDATE PDF to fix broken ones
                    await db.classes.update(existingClass.id!, {
                        pdfFile: blob,
                        pdfName: pdfName,
                        description: 'Lectura obligatoria sobre el impacto de la IA en la educación.'
                    });
                    console.log('🔄 Class PDF Refreshed (Local File)');
                }

            } catch (e) {
                console.error('❌ Failed to load local PDF:', e);
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
