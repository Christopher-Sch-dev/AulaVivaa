import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { DataService } from '../services/data';
import { type ClassSession } from '../db/db';
import { Button } from '../components/ui';
import { ArrowLeft, MonitorPlay, FileText, Download } from 'lucide-react';
import { AIChat } from '../components/AIChat';


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
    <div className="h-[calc(100vh-120px)] flex flex-col gap-6">

      {/* Header Info */}
      <div className="flex items-center justify-between flex-shrink-0 bg-surface/30 p-4 rounded-xl border border-white/5 backdrop-blur-sm">
        <div className="flex items-center gap-4">
          <Button variant="ghost" onClick={() => navigate(-1)} className="rounded-full w-10 h-10 p-0 flex items-center justify-center border-none bg-white/5 hover:bg-white/10">
            <ArrowLeft size={20} />
          </Button>
          <div>
            <h1 className="text-xl font-bold text-white tracking-tight">{session.name}</h1>
            <p className="text-muted text-sm font-sans">{session.description}</p>
          </div>
        </div>
        <div className="hidden sm:block">
          <span className="text-xs bg-primary/10 text-primary px-3 py-1 rounded-full border border-primary/20 font-mono">
            IA HABILITADA
          </span>
        </div>
      </div>

      <div className="flex-1 grid grid-cols-1 lg:grid-cols-2 gap-6 min-h-0">
        {/* PDF Viewer */}
        <div className="bg-surface/30 backdrop-blur-md rounded-2xl border border-white/5 flex flex-col overflow-hidden shadow-2xl relative group hover:border-white/10 transition-colors">
          <div className="p-3 border-b border-white/5 bg-black/20 flex justify-between items-center text-xs text-muted font-sans cursor-default select-none">
            <span className="flex items-center gap-2"><FileText size={14} className="text-secondary" /> Documento Fuente</span>
            <a href={pdfUrl!} download={session.pdfName} className="flex items-center gap-1 hover:text-white transition-colors">
              <Download size={12} /> Descargar
            </a>
          </div>
          {pdfUrl ? (
            <iframe
              src={pdfUrl}
              className="w-full h-full flex-1 bg-white/5"
              title="PDF Viewer"
            />
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center text-muted">
              <MonitorPlay size={48} className="mb-4 opacity-30" />
              <span className="text-sm">Sin documento visualizable</span>
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
