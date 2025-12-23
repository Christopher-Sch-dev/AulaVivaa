import React, { useState, useEffect, useRef } from 'react';
import * as pdfjsLib from 'pdfjs-dist';
import { Button, Input, Card } from './ui';
import { Send, Bot, Loader2, ShieldAlert, Key, Sparkles, FileText, HelpCircle, Lightbulb, Calendar } from 'lucide-react';
import { toast } from 'sonner';
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

  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  useEffect(() => {
    const extractText = async () => {
      try {
        setLoading(true);
        const arrayBuffer = await pdfFile.arrayBuffer();
        const pdf = await pdfjsLib.getDocument({ data: arrayBuffer }).promise;
        let fullText = '';

        for (let i = 1; i <= pdf.numPages; i++) {
          const page = await pdf.getPage(i);
          const textContent = await page.getTextContent();
          const pageText = textContent.items.map((item: any) => item.str).join(' ');
          fullText += `\n--- Page ${i} ---\n${pageText}`;
        }
        setPdfText(fullText);
        setPdfText(fullText);
        // Removed initial message to show Contextual Suggestions UI instead
        // setMessages([{ role: 'model', text: '...' }]); 
      } catch (error) {
        console.error('Error parsing PDF:', error);
        setMessages([{ role: 'model', text: '**Error Crítico**: No se pudo leer el PDF. Verifica que el archivo contenta texto seleccionable.' }]);
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
      const prompt = `
        Actúa como un profesor tutor experto del sistema.
        CONTEXTO (${pdfText.length} chars):
        ${pdfText.substring(0, 48000)}

        PREGUNTA:
        ${userMsg}

        Responde usando Markdown enriquecido (encabezados, listas, negritas, bloques de código si aplica). Sé claro, pedagógico y calmado.
      `;

      const { text } = await AIService.generateContent(apiKey, prompt);

      setMessages(prev => [...prev, { role: 'model', text }]);
      // toast.success(`Respondido con ${model}`, { position: 'bottom-right' }); 

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
              placeholder="Tu API Key aquí"
              onChange={(e) => setApiKey(e.target.value)}
              className="text-center font-mono tracking-wider bg-black/20"
            />
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
          ASISTENTE DE APRENDIZAJE
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
              {[
                { icon: FileText, label: "Resumir conceptos clave", text: "Resumir conceptos clave" },
                { icon: HelpCircle, label: "Generar 5 preguntas", text: "Generar 5 preguntas de quiz" },
                { icon: Lightbulb, label: "Explicar para niño de 5 años", text: "Explicar como si tuviera 5 años" },
                { icon: Calendar, label: "Crear plan de estudio", text: "Crear plan de estudio" }
              ].map((item, i) => (
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
