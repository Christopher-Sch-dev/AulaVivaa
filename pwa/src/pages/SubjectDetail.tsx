import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useStore } from '../store/useStore';
import { DataService } from '../services/data';
import { type ClassSession } from '../db/db';
import { Button, Card, Input } from '../components/ui';
import { FileText, ArrowLeft, Plus, Calendar, Upload } from 'lucide-react';
import { GlitchText } from '../components/GlitchText';
import { toast } from 'sonner';

export const SubjectDetail = () => {
  const { id } = useParams();
  const { user } = useStore();
  const navigate = useNavigate();
  const [classes, setClasses] = useState<ClassSession[]>([]);
  const [showModal, setShowModal] = useState(false);

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [date, setDate] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (id) loadClasses();
  }, [id]);

  const loadClasses = async () => {
    if (id) {
      const data = await DataService.getClassesBySubject(Number(id));
      setClasses(data);
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!file || !id) return;
    setLoading(true);

    try {
      if (name.length < 5) {
        toast.error('El título debe ser más descriptivo');
        setLoading(false);
        return;
      }

      const MAX_SIZE = 10 * 1024 * 1024; // 10MB
      if (file.size > MAX_SIZE) {
        toast.error('El archivo PDF excede el límite de 10MB');
        setLoading(false);
        return;
      }
      await DataService.createClass({
        name,
        description,
        date,
        pdfFile: file,
        pdfName: file.name,
        subjectId: Number(id)
      });
      setShowModal(false);
      setName('');
      setDescription('');
      setDate('');
      setFile(null);
      toast.success('Clase creada exitosamente');
      loadClasses();
    } catch (error) {
      console.error(error);
      toast.error('Error al crear la clase');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div className="flex items-center gap-4 mb-8">
        <Button variant="ghost" onClick={() => navigate('/')} className="rounded-full p-3 h-auto">
          <ArrowLeft size={24} />
        </Button>
        <GlitchText text="CONTENIDO DEL CURSO" className="text-3xl font-bold text-white uppercase" />
        {user?.role === 'docente' && (
          <Button onClick={() => setShowModal(true)} className="ml-auto flex items-center gap-2">
            <Plus size={18} /> <span className="hidden sm:inline">AGREGAR CLASE</span>
          </Button>
        )}
      </div>

      <div className="grid gap-4">
        {classes.map(cls => (
          <Card key={cls.id} className="flex items-center justify-between group hover:border-primary cursor-pointer transition-all active:scale-[0.99]" onClick={() => navigate(`/clase/${cls.id}`)}>
            <div className="flex items-center gap-6">
              <div className="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center text-primary group-hover:bg-primary group-hover:text-black transition-colors">
                <FileText size={24} />
              </div>
              <div>
                <h3 className="font-bold text-lg group-hover:text-primary transition-colors">{cls.name}</h3>
                <p className="text-gray-400 text-sm flex items-center gap-2 font-mono mt-1">
                  <Calendar size={14} /> {cls.date}
                </p>
              </div>
            </div>
            <Button variant="ghost" onClick={(e) => { e.stopPropagation(); navigate(`/clase/${cls.id}`); }}>
              ENTRAR
            </Button>
          </Card>
        ))}
        {classes.length === 0 && (
          <p className="text-gray-500 text-center py-20 border border-dashed border-gray-800 rounded">Aún no hay clases registradas.</p>
        )}
      </div>

      {showModal && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <Card className="w-full max-w-md border-primary/50 shadow-[0_0_50px_rgba(0,255,65,0.1)]">
            <h2 className="text-2xl font-bold mb-6 text-primary font-mono tracking-tight">NUEVA CLASE</h2>
            <form onSubmit={handleCreate} className="space-y-4">
              <div className="space-y-1">
                <label className="text-xs text-secondary font-bold ml-1">TÍTULO DE LA SESIÓN</label>
                <Input
                  placeholder="Ej: Introducción a la Mitosis"
                  value={name}
                  onChange={e => setName(e.target.value)}
                  required
                />
              </div>

              <div className="space-y-1">
                <label className="text-xs text-secondary font-bold ml-1">RESUMEN</label>
                <Input
                  placeholder="Ej: Análisis de las fases del ciclo celular..."
                  value={description}
                  onChange={e => setDescription(e.target.value)}
                  required
                />
              </div>
              <Input
                type="date"
                value={date}
                onChange={e => setDate(e.target.value)}
                required
              />

              <div className="relative border-2 border-dashed border-gray-700 rounded-lg p-6 text-center hover:border-primary hover:bg-primary/5 transition-all cursor-pointer group">
                <input
                  type="file"
                  accept="application/pdf"
                  onChange={e => setFile(e.target.files?.[0] || null)}
                  required
                  className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                />
                <div className="flex flex-col items-center gap-2">
                  <Upload size={32} className="text-gray-500 group-hover:text-primary transition-colors" />
                  <p className="text-sm text-gray-400 group-hover:text-white font-mono">
                    {file ? file.name : "Click para subir PDF"}
                  </p>
                </div>
              </div>
              <p className="text-[10px] text-gray-500 text-center">
                Formato permitido: <strong>PDF</strong> (Max 10MB). Este documento será analizado por la IA.
              </p>

              <div className="flex justify-end gap-2 mt-6">
                <Button type="button" variant="ghost" onClick={() => setShowModal(false)}>CANCELAR</Button>
                <Button type="submit" disabled={loading}>
                  {loading ? 'SUBIENDO...' : 'CREAR CLASE'}
                </Button>
              </div>
            </form>
          </Card>
        </div>
      )}
    </div>
  );
};
