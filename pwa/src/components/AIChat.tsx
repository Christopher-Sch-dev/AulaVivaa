import React, { useState, useEffect, useRef } from 'react';
import * as pdfjsLib from 'pdfjs-dist';
import { createWorker } from 'tesseract.js';
import { Button, Input, Card } from './ui';
import { Send, Bot, Loader2, ShieldAlert, Key, Sparkles, FileText, HelpCircle, Lightbulb, Calendar } from 'lucide-react';
import { toast } from 'sonner';
import { useStore } from '../store/useStore';
import { AIService } from '../services/ai';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeRaw from 'rehype-raw';

pdfjsLib.GlobalWorkerOptions.workerSrc = '/pdf.worker.min.mjs';

interface AIChatProps {
  pdfFile: Blob;
  className?: string;
}

interface Message {
  role: 'user' | 'model';
  text: string;
}

export const AIChat = ({ pdfFile, className }: AIChatProps) => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [pdfText, setPdfText] = useState('');
  const [apiKey, setApiKey] = useState(localStorage.getItem('GEMINI_API_KEY') || '');
  const [showKeyInput, setShowKeyInput] = useState(!apiKey);

  const { user } = useStore();
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  useEffect(() => {
    // ---------------------------------------------------------
    // 1. FILE HASHING (Deep Identity)
    // ---------------------------------------------------------
    const generateFileHash = async (buffer: ArrayBuffer) => {
      const hashBuffer = await crypto.subtle.digest('SHA-256', buffer);
      const hashArray = Array.from(new Uint8Array(hashBuffer));
      return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
    };

    // ---------------------------------------------------------
    // 2. OCR GOD MODE (Visual Vision)
    // ---------------------------------------------------------
    const performOCR = async (pdfDoc: any) => {
      setLoading(true);
      toast.info('🔍 PDF Escaneado Detectado. Activando Visión Artificial (OCR)...');
      setMessages(prev => [...prev, { role: 'model', text: '> **MODO DIOS (OCR) ACTIVADO** 👁️\n\nEste documento es una imagen. Estoy usando mis redes neuronales visuales para leerlo pixel a pixel. Esto tomará unos segundos.' }]);

      let ocrText = '';
      const worker = await createWorker('spa'); // Spanish trained data

      try {
        const totalPages = pdfDoc.numPages;
        for (let i = 1; i <= totalPages; i++) {
          const page = await pdfDoc.getPage(i);
          const viewport = page.getViewport({ scale: 2.0 }); // High scale for better recognition
          const canvas = document.createElement('canvas');
          canvas.width = viewport.width;
          canvas.height = viewport.height;

          const context = canvas.getContext('2d');
          if (context) {
            await page.render({ canvasContext: context, viewport }).promise;

            // Get text from image
            const { data: { text } } = await worker.recognize(canvas);

            ocrText += `\n--- PÁGINA ${i} (OCR) ---\n${text}`;
            toast.loading(`Leyendo página ${i}/${totalPages} (IA Vision)...`);
          }
        }
      } catch (err) {
        console.error('OCR Error:', err);
        toast.error('Falló el OCR. El archivo podría estar dañado.');
      } finally {
        await worker.terminate();
        toast.dismiss();
      }
      return ocrText;
    };

    const extractText = async () => {
      try {
        setLoading(true);
        const arrayBuffer = await pdfFile.arrayBuffer();

        // Hashing for Uniqueness
        const fileHash = await generateFileHash(arrayBuffer);
        console.log(`[PDF] ID Único (SHA-256): ${fileHash}`);

        const pdf = await pdfjsLib.getDocument({ data: arrayBuffer }).promise;
        let fullSmartText = '';
        let fullRawText = '';
        let totalPages = pdf.numPages;

        console.log(`[PDF] Iniciando extracción híbrida de ${totalPages} páginas...`);

        // TIER 1 & 2: TEXT EXTRACTION
        for (let i = 1; i <= totalPages; i++) {
          try {
            const page = await pdf.getPage(i);
            const textContent = await page.getTextContent();
            const textItems = textContent.items.filter((item: any) => item.str != null);

            // --- Strategy A: Smart Layout (Sort by Y, then X) ---
            const itemsWithPos = textItems.filter((item: any) => Array.isArray(item.transform));

            const sortedItems = itemsWithPos.map((item: any) => ({
              str: item.str,
              y: item.transform[5],
              x: item.transform[4],
              hasEOL: item.hasEOL
            })).sort((a, b) => {
              if (Math.abs(a.y - b.y) > 5) return b.y - a.y;
              return a.x - b.x;
            });

            let pageSmartText = '';
            let lastY = -1;
            sortedItems.forEach((item) => {
              if (lastY === -1) lastY = item.y;
              if (Math.abs(item.y - lastY) > 5) {
                pageSmartText += '\n';
                lastY = item.y;
              } else if (pageSmartText.length > 0 && !pageSmartText.endsWith('\n') && !pageSmartText.endsWith(' ')) {
                if (item.str.trim().length > 0) pageSmartText += ' ';
              }
              pageSmartText += item.str;
            });
            fullSmartText += `\n--- PÁGINA ${i} ---\n${pageSmartText}`;

            // --- Strategy B: Raw Stream (Backup) ---
            const pageRawText = textItems.map((item: any) => item.str).join(' ');
            fullRawText += `\n--- PÁGINA ${i} ---\n${pageRawText}`;

          } catch (pageError) {
            console.error(`Error leyendo página ${i}:`, pageError);
          }
        }

        const smartLen = fullSmartText.replace(/--- PÁGINA \d+ ---/g, '').trim().length;
        const rawLen = fullRawText.replace(/--- PÁGINA \d+ ---/g, '').trim().length;

        let finalText = fullSmartText;
        if (smartLen < (rawLen * 0.5)) {
          console.warn('[PDF] Smart Extraction perdió contenido. Usando Raw Fallback.');
          finalText = fullRawText;
        }

        // TIER 3: GOD MODE (OCR)
        const finalLen = finalText.replace(/--- PÁGINA \d+ ---/g, '').trim().length;

        if (finalLen < 50) {
          console.warn('[PDF] Texto insuficiente. Activando OCR Tesseract...');
          const ocrResult = await performOCR(pdf);

          if (ocrResult.replace(/--- PÁGINA \d+ \(OCR\) ---/g, '').trim().length > 50) {
            finalText = ocrResult;
            toast.success('¡Lectura OCR completada!');
          } else {
            toast.error('⚠️ El documento es ilegible incluso con OCR.');
            setMessages(prev => [...prev, { role: 'model', text: '**ERROR FATAL**: No pude leer nada, ni siquiera con scaneo visual. El archivo podría estar en blanco o muy dañado.' }]);
          }
        }

        setPdfText(finalText);

      } catch (error) {
        console.error('Error parsing PDF:', error);
        setMessages([{ role: 'model', text: '**Error Crítico**: No se pudo procesar el archivo PDF.' }]);
      } finally {
        setLoading(false);
      }
    };

    if (pdfFile) extractText();
  }, [pdfFile]);

  const handleSend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() || !apiKey) return;

    const userMsg = input;
    setInput('');
    setMessages(prev => [...prev, { role: 'user', text: userMsg }]);
    setLoading(true);

    try {
      const roleContext = user?.role === 'docente'
        ? `ERES UN ASISTENTE PEDAGÓGICO EXPERTO (Tutor para Docentes).
           TUS DEBERES:
           1. Enseñar metodologías y estrategias didácticas.
           2. Retroalimentar el material: Sugerir mejoras, detectar huecos de contenido.
           3. Ayudar a preparar clases: Estructurar por bloques de tiempo, sugerir actividades interactivas.
           4. Generar evaluaciones: Tests, quizzes, rúbricas.
           TONO: Profesional, colega experto, estructurado, directo.`
        : `ERES UN TUTOR DE ESTUDIO PERSONALIZADO (Tutor para Alumnos).
           TUS DEBERES:
           1. Enseñar: Explicar conceptos complejos de forma simple.
           2. Resumir: Extraer las ideas clave para facilitar el estudio.
           3. Hacer preguntas: Interrogar al alumno (método socrático) para verificar entendimiento.
           4. Adaptabilidad: Explicar "como si tuviera 5 años" o con analogías si se pide.
           TONO: Amigable, motivador, paciente, mentor inspirador.`;

      const prompt = `
        ROL: ${roleContext}
        
        CONTEXTO DEL DOCUMENTO (${pdfText.length} chars):
        ${pdfText}

        PREGUNTA DEL USUARIO (${user?.name || 'Usuario'}):
        ${userMsg}

        INSTRUCCIONES DE FORMATO:
        - Responde usando Markdown enriquecido.
        - Usa listas, negritas y tablas si es útil.
        - Identifica explícitamente secciones clave.
      `;

      const { text } = await AIService.generateContent(apiKey, prompt);

      setMessages(prev => [...prev, { role: 'model', text }]);

    } catch (error: any) {
      if (error.message.includes('API Key')) {
        setMessages(prev => [...prev, { role: 'model', text: '> **ERROR DE ACCESO**\n\nTu API Key no es válida o ha expirado.' }]);
        setShowKeyInput(true);
      } else {
        setMessages(prev => [...prev, { role: 'model', text: `> **ERROR DEL SISTEMA**\n\n${error.message}` }]);
      }
    } finally {
      setLoading(false);
    }
  };

  const saveKey = (key: string) => {
    if (!key.trim()) {
      toast.error('Token vacío');
      return;
    }
    setApiKey(key);
    localStorage.setItem('GEMINI_API_KEY', key);
    setShowKeyInput(false);
    toast.success('Token Guardado');
  };

  const suggestions = user?.role === 'docente' ? [
    { icon: Calendar, label: "Planificar estructura de clase", text: "Genera una estructura de clase de 90 min basada en este contenido, con tiempos y actividades." },
    { icon: HelpCircle, label: "Crear quiz de 5 preguntas", text: "Genera un quiz de 5 preguntas de selección múltiple con solucionario para evaluar este texto." },
    { icon: Lightbulb, label: "Ideas de actividades prácticas", text: "Sugiere 3 actividades dinámicas grupales para enseñar este tema." },
    { icon: FileText, label: "Generar Rúbrica de evaluación", text: "Crea una rúbrica detallada para evaluar un trabajo sobre este tema." }
  ] : [
    { icon: FileText, label: "Resumir para estudiar", text: "Hazme un resumen estructurado con lo más importante para estudiar para el examen." },
    { icon: HelpCircle, label: "Ponme a prueba (Quiz)", text: "Hazme 3 preguntas difíciles sobre esto para ver si entendí. No me des las respuestas todavía." },
    { icon: Lightbulb, label: "Explicar como a un niño", text: "Explícame este concepto clave como si tuviera 5 años, usando analogías divertidas." },
    { icon: Calendar, label: "Crear plan de estudio", text: "Organiza un plan de estudio de 3 días para aprender esto paso a paso." }
  ];

  if (showKeyInput) {
    return (
      <Card className={`h-full flex flex-col justify-center items-center p-8 text-center space-y-6 bg-surface/90 backdrop-blur-md border border-primary/20 ${className}`}>
        <div className="relative group">
          <div className="absolute inset-0 bg-primary/20 blur-xl rounded-full animate-pulse group-hover:bg-primary/30 transition-all" />
          <Bot size={64} className="text-primary relative z-10" />
        </div>

        <div className="space-y-2">
          <h3 className="text-xl font-bold text-white tracking-tight">AUTENTICACIÓN REQUERIDA</h3>
          <p className="text-muted text-sm max-w-xs mx-auto">
            Para activar el Módulo IA, ingresa tu credencial de Gemini.
          </p>
        </div>

        <div className="w-full max-w-sm space-y-4">
          <div className="bg-primary/5 border border-primary/10 rounded-lg p-3 flex items-start gap-3 text-left">
            <ShieldAlert className="text-primary shrink-0 mt-0.5" size={18} />
            <p className="text-xs text-muted">
              <span className="font-bold text-primary">PRIVACIDAD:</span> Los datos viajan encriptados directamente a Google. Sin intermediarios.
            </p>
          </div>

          <div className="space-y-3">
            <Input
              type="password"
              placeholder="Tu API Key de Gemini"
              onChange={(e) => setApiKey(e.target.value)}
              className="text-center font-mono tracking-wider bg-black/20"
              autoComplete="off"
            />
            <p className="text-[10px] text-gray-500 text-center">
              * Tu llave se guarda localmente en este dispositivo.
            </p>
            <Button onClick={() => saveKey(apiKey)} className="w-full flex items-center justify-center gap-2">
              <Key size={16} /> VINCULAR SISTEMA
            </Button>
          </div>
          <a href="https://aistudio.google.com/app/apikey" target="_blank" rel="noreferrer" className="block text-xs text-secondary hover:text-white transition-colors">
            ¿No tienes una? Consíguela gratis aquí
          </a>
        </div>
      </Card>
    );
  }

  return (
    <div className={`flex flex-col h-[600px] bg-surface/50 border border-white/5 rounded-xl shadow-2xl relative overflow-hidden backdrop-blur-sm ${className}`}>

      {/* Header Calmeante */}
      <div className="px-6 py-4 border-b border-white/5 flex justify-between items-center bg-black/20">
        <div className="flex items-center gap-3 text-primary font-medium tracking-wide">
          <Sparkles size={18} className="text-secondary animate-pulse" />
          ASISTENTE DE APRENDIZAJE ({user?.role === 'docente' ? 'PEDAGÓGICO' : 'TUTOR'})
        </div>
        <button onClick={() => setShowKeyInput(true)} className="text-[10px] uppercase font-bold text-muted hover:text-white border border-white/10 px-3 py-1 rounded-full hover:bg-white/5 transition-all">
          Ajustes
        </button>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-6 scrollbar-thin scrollbar-thumb-white/10 relative z-10">

        {/* Contextual Suggestions (Empty State) */}
        {messages.length === 0 && !loading && (
          <div className="flex flex-col items-center justify-center h-full text-center p-6 space-y-6 opacity-80">
            <Bot size={48} className="text-white/20 mb-2" />
            <p className="text-sm text-gray-400 max-w-xs">
              Hola, soy tu Tutor IA. Basado en el documento, te sugiero:
            </p>
            <div className="grid grid-cols-1 gap-3 w-full max-w-sm">
              {suggestions.map((item, i) => (
                <button
                  key={i}
                  onClick={() => setInput(item.text)}
                  className="text-left text-xs p-3 rounded-lg bg-white/5 hover:bg-white/10 border border-white/5 hover:border-primary/50 transition-all text-gray-300 hover:text-white flex items-center gap-3 group"
                >
                  <item.icon size={14} className="text-primary opacity-70 group-hover:scale-110 transition-transform" />
                  <span>{item.label}</span>
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map((msg, idx) => (
          <div key={idx} className={`flex gap-4 ${msg.role === 'user' ? 'justify-end' : 'justify-start'} animate-in slide-in-from-bottom-2 duration-500 fade-in-5`}>

            {msg.role === 'model' && (
              <div className="w-8 h-8 rounded-full bg-linear-to-br from-primary to-secondary flex items-center justify-center shrink-0 mt-1 shadow-lg shadow-primary/20">
                <Bot size={16} className="text-white" />
              </div>
            )}

            <div className={`max-w-[85%] p-5 rounded-2xl text-sm leading-relaxed shadow-sm ${msg.role === 'user'
              ? 'bg-primary text-white rounded-tr-sm shadow-primary/10'
              : 'bg-black/30 text-gray-200 border border-white/5 rounded-tl-sm backdrop-blur-md'
              }`}>
              {msg.role === 'model' ? (
                <div className="prose prose-invert prose-sm max-w-none prose-p:my-2 prose-headings:text-secondary prose-a:text-primary prose-code:bg-black/50 prose-code:px-1 prose-code:rounded prose-pre:bg-black/50 prose-pre:border prose-pre:border-white/5">
                  <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeRaw]}>
                    {msg.text}
                  </ReactMarkdown>
                </div>
              ) : (
                <p>{msg.text}</p>
              )}
            </div>

            {msg.role === 'user' && (
              <div className="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center shrink-0 mt-1">
                <div className="w-4 h-4 rounded-full bg-white/50" />
              </div>
            )}
          </div>
        ))}

        {loading && (
          <div className="flex items-start gap-4 animate-pulse">
            <div className="w-8 h-8 rounded-full bg-primary/20 flex items-center justify-center shrink-0">
              <Loader2 size={16} className="text-primary animate-spin" />
            </div>
            <div className="bg-black/20 px-4 py-3 rounded-2xl rounded-tl-sm text-xs text-muted flex items-center gap-2 border border-white/5">
              Pensando...
            </div>
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Quiet Input Area */}
      <form onSubmit={handleSend} className="p-4 bg-black/20 flex gap-3 relative z-20 border-t border-white/5">
        <Input
          value={input}
          onChange={e => setInput(e.target.value)}
          disabled={loading}
          className="flex-1 bg-black/40 border-white/10 focus:border-primary/50 focus:ring-primary/20 rounded-xl placeholder-gray-600 transition-all font-sans"
          placeholder="Escribe aquí para preguntar..."
        />
        <Button type="submit" disabled={loading} className="w-12 h-12 rounded-xl p-0 flex items-center justify-center bg-primary hover:bg-primary/90 shadow-lg shadow-primary/20 transition-all">
          {loading ? <Loader2 className="animate-spin text-white" size={20} /> : <Send size={20} className="text-white ml-0.5" />}
        </Button>
      </form>
    </div>
  );
};
