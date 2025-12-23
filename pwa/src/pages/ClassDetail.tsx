import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { DataService } from '../services/data';
import { type ClassSession } from '../db/db';
import { Button } from '../components/ui';
import { ArrowLeft, MonitorPlay, FileText } from 'lucide-react';
import { AIChat } from '../components/AIChat';
import { GlitchText } from '../components/GlitchText';

export const ClassDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [session, setSession] = useState<ClassSession | undefined>(undefined);
  const [pdfUrl, setPdfUrl] = useState<string | null>(null);

  useEffect(() => {
    loadClass();
    return () => {
      if (pdfUrl) URL.revokeObjectURL(pdfUrl);
    }
  }, [id]);

  const loadClass = async () => {
    if (id) {
      const data = await DataService.getClassById(Number(id));
      setSession(data);
      if (data?.pdfFile) {
        const url = URL.createObjectURL(data.pdfFile);
        setPdfUrl(url);
      }
    }
  };

  if (!session) return <div className="text-center py-40 text-primary font-mono animate-pulse">CARGANDO RECURSOS...</div>;

  return (
    <div className="h-[calc(100vh-100px)] flex flex-col">
      <div className="flex items-center gap-4 mb-6 flex-shrink-0">
        <Button variant="ghost" onClick={() => navigate(-1)} className="rounded-full w-10 h-10 p-0 flex items-center justify-center">
          <ArrowLeft size={20} />
        </Button>
        <div>
          <GlitchText text={session.name.toUpperCase()} className="text-2xl font-bold text-white mb-1 block" />
          <p className="text-gray-400 text-xs font-mono uppercase tracking-widest">{session.description}</p>
        </div>
      </div>

      <div className="flex-1 grid grid-cols-1 lg:grid-cols-2 gap-6 min-h-0 pb-6">
        {/* PDF Viewer */}
        <div className="bg-black/40 backdrop-blur-md rounded-xl border border-gray-800 flex flex-col overflow-hidden shadow-2xl relative group">
          {/* Tech Corners */}
          <div className="absolute top-0 left-0 w-4 h-4 border-t border-l border-primary/50 rounded-tl z-20" />
          <div className="absolute top-0 right-0 w-4 h-4 border-t border-r border-primary/50 rounded-tr z-20" />
          <div className="absolute bottom-0 left-0 w-4 h-4 border-b border-l border-primary/50 rounded-bl z-20" />
          <div className="absolute bottom-0 right-0 w-4 h-4 border-b border-r border-primary/50 rounded-br z-20" />

          <div className="p-3 border-b border-gray-800 bg-black/60 flex justify-between items-center text-xs text-gray-400 font-mono">
            <span className="flex items-center gap-2"><FileText size={14} className="text-primary" /> {session.pdfName}</span>
            <a href={pdfUrl!} download={session.pdfName} className="hover:text-primary hover:underline decoration-primary transition-all">DESCARGAR_ARCHIVO</a>
          </div>
          {pdfUrl ? (
            <iframe
              src={pdfUrl}
              className="w-full h-full flex-1 bg-white/5"
              title="PDF Viewer"
            />
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center text-gray-600">
              <MonitorPlay size={48} className="mb-4 opacity-50" />
              <span className="font-mono text-sm">NO_SIGNAL_FOUND</span>
            </div>
          )}
        </div>

        {/* AI Chat */}
        <div className="h-full min-h-0">
          {session.pdfFile && <AIChat pdfFile={session.pdfFile} className="h-full" />}
        </div>
      </div>
    </div>
  );
};
