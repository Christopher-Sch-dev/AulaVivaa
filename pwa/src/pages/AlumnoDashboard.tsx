import React, { useEffect, useState } from 'react';
import { useStore } from '../store/useStore';
import { DataService } from '../services/data';
import { type Subject } from '../db/db';
import { Button, Card, Input } from '../components/ui';
import { BookOpen, Search, FileText } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { GlitchText } from '../components/GlitchText';
import { toast } from 'sonner';
import { StatCard } from '../components/StatCard';

export const AlumnoDashboard = () => {
  const { user } = useStore();
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [stats, setStats] = useState({ subjects: 0, classes: 0 });
  const [joinCode, setJoinCode] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    loadData();
  }, [user]);

  const loadData = async () => {
    if (user?.id) {
      const [subjectsData, statsData] = await Promise.all([
        DataService.getStudentSubjects(user.id),
        DataService.getStudentStats(user.id)
      ]);
      setSubjects(subjectsData);
      setStats(statsData);
    }
  };

  const handleJoin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user?.id) return;

    if (joinCode.trim().length < 5) {
      toast.error('Código de asignatura inválido');
      return;
    }

    try {
      await DataService.joinSubject(user.id, joinCode);
      setJoinCode('');
      toast.success('¡Inscripción exitosa!');
      loadData();
    } catch (err: any) {
      toast.error(err.message);
    }
  };

  return (
    <div>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-12">
        <StatCard
          label="Asignaturas Inscritas"
          value={stats.subjects}
          icon={BookOpen}
          color="#7C3AED"
          delay={0}
        />
        <StatCard
          label="Clases Disponibles"
          value={stats.classes}
          icon={FileText}
          color="#0D9488"
          delay={1}
        />
      </div>

      <div className="mb-12 p-6 rounded-xl border border-primary/20 bg-primary/5">
        <h2 className="text-xl font-bold mb-4 text-primary font-mono custom-kerning">INSCRIBIR ASIGNATURA</h2>
        <form onSubmit={handleJoin} className="flex flex-col sm:flex-row gap-4 max-w-2xl">
          <Input
            placeholder="Ingresa el código (ej: PRG2025-X9Y8)"
            value={joinCode}
            onChange={e => setJoinCode(e.target.value)}
            className="flex-1 bg-black/50"
          />
          <p className="text-[10px] text-gray-500 mt-1 sm:hidden">Pídele el código de acceso a tu profesor.</p>
          <Button type="submit" className="flex items-center justify-center gap-2 sm:w-auto w-full">
            <Search size={18} />
            UNIRSE
          </Button>
        </form>
      </div>

      <GlitchText text="MIS INSCRIPCIONES" className="text-3xl font-bold text-white mb-8 block" />

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {subjects.map(subject => (
          <Card key={subject.id} className="cursor-pointer group hover:bg-white/5" onClick={() => navigate(`/asignatura/${subject.id}`)}>
            <div className="flex items-start justify-between mb-4">
              <BookOpen className="text-emerald-500 group-hover:text-primary transition-colors" size={32} />
              <span className="text-xs bg-emerald-900/40 text-emerald-400 px-2 py-1 rounded font-mono border border-emerald-900">
                ALUMNO
              </span>
            </div>
            <h3 className="text-xl font-bold group-hover:text-primary transition-colors">{subject.name}</h3>
            <p className="text-gray-400 mt-2 line-clamp-2 text-sm">{subject.description}</p>
          </Card>
        ))}
        {subjects.length === 0 && (
          <div className="col-span-full text-center py-20 text-gray-500 border border-dashed border-gray-800 rounded-lg">
            <BookOpen size={48} className="mb-4 opacity-50 mx-auto" />
            <p>No estás inscrito en ninguna asignatura.</p>
          </div>
        )}
      </div>
    </div>
  );
};
