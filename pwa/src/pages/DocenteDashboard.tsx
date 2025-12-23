import React, { useEffect, useState } from 'react';
import { useStore } from '../store/useStore';
import { DataService } from '../services/data';
import { type Subject } from '../db/db';
import { Button, Card, Input } from '../components/ui';
import { Plus, BookOpen, Copy, Check } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { GlitchText } from '../components/GlitchText';
import confetti from 'canvas-confetti';

export const DocenteDashboard = () => {
  const { user } = useStore();
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [showModal, setShowModal] = useState(false);
  const [newSubject, setNewSubject] = useState({ name: '', description: '' });
  const [copiedId, setCopiedId] = useState<number | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    loadSubjects();
  }, [user]);

  const loadSubjects = async () => {
    if (user?.id) {
      const data = await DataService.getSubjectsByTeacher(user.id);
      setSubjects(data);
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user?.id) return;
    
    const randomSuffix = Math.random().toString(36).substring(2, 6).toUpperCase();
    const code = `${newSubject.name.substring(0, 3).toUpperCase()}2025-${randomSuffix}`;

    await DataService.createSubject({
      ...newSubject,
      code,
      teacherId: user.id
    });

    confetti({ particleCount: 100, spread: 70, origin: { y: 0.6 }, colors: ['#00FF41', '#00AD2F'] });
    toast.success('Asignatura creada correctamente');

    setShowModal(false);
    setNewSubject({ name: '', description: '' });
    loadSubjects();
  };

  const copyCode = (e: React.MouseEvent, code: string, id: number) => {
    e.stopPropagation();
    navigator.clipboard.writeText(code);
    toast.success('Código copiado al portapapeles');
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-8">
        <GlitchText text="MIS ASIGNATURAS" className="text-3xl font-bold text-primary" />
        <Button onClick={() => setShowModal(true)} className="flex items-center gap-2 shadow-[0_0_15px_rgba(0,255,65,0.3)]">
          <Plus size={20} />
          <span className="hidden sm:inline">NUEVA ASIGNATURA</span>
        </Button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {subjects.map(subject => (
          <Card key={subject.id} className="cursor-pointer group relative overflow-hidden active:scale-95 duration-200 border-gray-800 hover:border-primary" onClick={() => navigate(`/asignatura/${subject.id}`)}>
            <div className="absolute inset-0 bg-gradient-to-br from-primary/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
            
            <div className="flex items-start justify-between mb-4 relative z-10">
              <BookOpen className="text-primary" size={32} />
              <button 
                onClick={(e) => copyCode(e, subject.code, subject.id!)}
                className="flex items-center gap-2 text-xs font-mono bg-black/40 border border-primary/30 text-primary px-3 py-1.5 rounded hover:bg-primary/20 transition-colors"
              >
                {subject.code}
                {copiedId === subject.id ? <Check size={14} /> : <Copy size={14} />}
              </button>
            </div>
            
            <h3 className="text-xl font-bold group-hover:text-primary transition-colors relative z-10">{subject.name}</h3>
            <p className="text-gray-400 mt-2 line-clamp-2 text-sm relative z-10">{subject.description}</p>
          </Card>
        ))}
        {subjects.length === 0 && (
             <div className="col-span-full flex flex-col items-center justify-center py-20 text-gray-500 border border-dashed border-gray-800 rounded-lg">
                <BookOpen size={48} className="mb-4 opacity-50" />
                <p>No has creado ninguna asignatura aún.</p>
             </div>
        )}
      </div>

      {showModal && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <Card className="w-full max-w-md animate-in fade-in zoom-in duration-300 border-primary/50 shadow-[0_0_50px_rgba(0,255,65,0.1)]">
            <h2 className="text-2xl font-bold mb-4 text-primary font-mono tracking-tight">NUEVA ASIGNATURA</h2>
            <form onSubmit={handleCreate} className="space-y-4">
              <Input 
                placeholder="Nombre (ej: Programación)" 
                value={newSubject.name} 
                onChange={e => setNewSubject({...newSubject, name: e.target.value})}
                required
                className="bg-black/50 border-gray-700 focus:border-primary"
              />
              <Input 
                placeholder="Descripción" 
                value={newSubject.description} 
                onChange={e => setNewSubject({...newSubject, description: e.target.value})}
                required
                className="bg-black/50 border-gray-700 focus:border-primary"
              />
              <div className="flex justify-end gap-2 mt-6">
                <Button type="button" variant="ghost" onClick={() => setShowModal(false)}>CANCELAR</Button>
                <Button type="submit">CREAR</Button>
              </div>
            </form>
          </Card>
        </div>
      )}
    </div>
  );
};
