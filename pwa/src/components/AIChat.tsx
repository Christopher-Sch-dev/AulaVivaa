import { useState, useEffect, useRef } from 'react';
import { GoogleGenerativeAI } from '@google/generative-ai';
import * as pdfjsLib from 'pdfjs-dist';
import { Button, Input, Card } from './ui';
import { Send, Bot, Loader2, ShieldAlert, Key } from 'lucide-react';
import { GlitchText } from './GlitchText';
import { toast } from 'sonner';

// Use local worker for offline support and reliability
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

  // Scroll to bottom
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // Extract PDF Text on mount/change
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
        setMessages([{ role: 'model', text: '¡LISTO! He analizado el documento. ¿Qué necesitas saber?' }]);
      } catch (error) {
        console.error('Error parsing PDF:', error);
        setMessages([{ role: 'model', text: 'ERROR CRÍTICO: No se pudo leer el PDF. Asegúrate de que sea texto seleccionable (no escaneado / imagen).' }]);
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
      const genAI = new GoogleGenerativeAI(apiKey);
      const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" });

      const prompt = `
        Actúa como un profesor experto y conciso del sistema "Aula Viva".
        Tienes el siguiente contexto extraído de un documento PDF:
        
        CONTEXTO (${pdfText.length} caracteres):
        ${pdfText.substring(0, 50000)}

        PREGUNTA DEL ESTUDIANTE:
        ${userMsg}

        INSTRUCCIONES:
        1. Responde de manera educativa y clara.
        2. Usa formato Markdown (negritas, listas) para estructurar la respuesta.
        3. Si la respuesta no está en el contexto, indícalo.
      `;

      const result = await model.generateContent(prompt);
      const response = result.response.text();

      setMessages(prev => [...prev, { role: 'model', text: response }]);
    } catch (error: any) {
      if (error.message.includes('API key')) {
        setMessages(prev => [...prev, { role: 'model', text: 'ERROR DE AUTENTICACIÓN: Tu API Key no es válida.' }]);
        setShowKeyInput(true);
      } else {
        setMessages(prev => [...prev, { role: 'model', text: `ERROR DEL SISTEMA: ${error.message}` }]);
      }
    } finally {
      setLoading(false);
    }
  };

  const saveKey = (key: string) => {
    if (!key.trim()) {
      toast.error('Ingresa una API Key válida');
      return;
    }
    setApiKey(key);
    localStorage.setItem('GEMINI_API_KEY', key);
    setShowKeyInput(false);
    toast.success('Clave de seguridad almacenada');
  };

  if (showKeyInput) {
    return (
      <Card className={`h-full flex flex-col justify-center items-center p-8 text-center space-y-6 border-accent/30 bg-black/80 backdrop-blur-xl ${className}`}>
        <div className="relative">
          <div className="absolute inset-0 bg-primary/20 blur-xl rounded-full animate-pulse" />
          <Bot size={64} className="text-primary relative z-10" />
        </div>

        <div className="space-y-2">
          <GlitchText text="SISTEMA IA: AUTH REQUERIDA" className="text-xl font-bold text-accent" />
          <p className="text-gray-400 text-sm max-w-xs mx-auto">
            Para interactuar con Gemini 1.5 Flash, se requiere una credencial de acceso.
          </p>
        </div>

        <div className="w-full max-w-sm space-y-4">
          <div className="bg-accent/5 border border-accent/20 rounded p-3 flex items-start gap-3 text-left">
            <ShieldAlert className="text-accent shrink-0 mt-0.5" size={18} />
            <p className="text-xs text-gray-300">
              <span className="font-bold text-accent">AVISO DE PRIVACIDAD:</span> Tu API Key se almacena <strong>únicamente en tu navegador</strong> (LocalStorage). No se envía a ningún servidor intermedio, solo directo a Google API. Es seguro usar una key gratuita.
            </p>
          </div>

          <div className="space-y-2">
            <Input
              type="password"
              placeholder="Pegar Google Gemini API Key"
              onChange={(e) => setApiKey(e.target.value)}
              className="bg-black/50 border-gray-700 text-center font-mono tracking-widest"
            />
            <Button onClick={() => saveKey(apiKey)} className="w-full flex items-center justify-center gap-2">
              <Key size={16} /> ACTIVAR SISTEMA
            </Button>
          </div>

          <a href="https://aistudio.google.com/app/apikey" target="_blank" rel="noreferrer" className="block text-xs text-primary hover:underline opacity-70 hover:opacity-100 transition-opacity">
            Obtener API Key gratuita (Google AI Studio) &rarr;
          </a>
        </div>
      </Card>
    );
  }

  return (
    <div className={`flex flex-col h-[600px] border border-gray-800 rounded-lg bg-black/80 backdrop-blur-xl shadow-2xl relative overflow-hidden ${className}`}>
      {/* Header */}
      <div className="p-4 border-b border-gray-800 flex justify-between items-center bg-black/40 z-10">
        <div className="flex items-center gap-2 text-primary font-bold font-mono tracking-tighter">
          <span className="w-2 h-2 bg-primary rounded-full animate-pulse" />
          AULA_VIVA_AI :: V2.0
        </div>
        <button onClick={() => setShowKeyInput(true)} className="text-[10px] uppercase font-bold text-gray-600 hover:text-accent border border-gray-800 px-2 py-1 rounded hover:border-accent transition-all">
          Reconfigurar Credenciales
        </button>
      </div>

      {/* Scanlines Overlay for Chat */}
      <div className="absolute inset-0 pointer-events-none opacity-[0.03] bg-[linear-gradient(rgba(18,16,16,0)_50%,rgba(0,0,0,0.25)_50%),linear-gradient(90deg,rgba(255,0,0,0.06),rgba(0,255,0,0.02),rgba(0,0,255,0.06))] bg-[length:100%_2px,3px_100%] z-0" />

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-6 scrollbar-thin scrollbar-thumb-primary/20 relative z-10">
        {messages.map((msg, idx) => (
          <div key={idx} className={`flex gap-3 ${msg.role === 'user' ? 'justify-end' : 'justify-start'} animate-in slide-in-from-bottom-2 duration-300`}>

            <div className={`max-w-[85%] p-4 rounded-xl text-sm leading-relaxed shadow-lg relative group ${msg.role === 'user'
              ? 'bg-primary/10 text-Primary-50 border border-primary/20 rounded-tr-none'
              : 'bg-surface/80 text-gray-200 border border-t border-gray-700/50 rounded-tl-none'
              }`}>
              {/* Decorative triangle */}
              <div className={`absolute top-0 w-2 h-2 ${msg.role === 'user' ? '-right-1.5 bg-primary/20' : '-left-1.5 bg-gray-700/50'} rotate-45 transform origin-center border-t border-l border-inherit`} />

              {msg.role === 'model' && (
                <div className="prose prose-invert prose-p:my-1 prose-headings:text-primary prose-strong:text-primary/90 text-sm max-w-none">
                  <div dangerouslySetInnerHTML={{ __html: msg.text.replace(/\n/g, '<br/>').replace(/\*\*(.*?)\*\*/g, '<b>$1</b>') }} />
                </div>
              )}
              {msg.role === 'user' && msg.text}

              <div className={`text-[9px] font-mono mt-1 opacity-40 uppercase tracking-wider ${msg.role === 'user' ? 'text-right' : 'text-left'}`}>
                {msg.role === 'user' ? 'User_Input' : 'AI_Response'}
              </div>
            </div>
          </div>
        ))}
        {loading && (
          <div className="flex justify-start gap-3">
            <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center animate-spin">
              <Loader2 size={16} className="text-primary" />
            </div>
            <div className="bg-surface/50 px-4 py-2 rounded-lg text-xs text-primary animate-pulse flex items-center gap-2">
              ANALIZANDO DATOS <span className="flex gap-0.5"><span className="animate-bounce">.</span><span className="animate-bounce delay-100">.</span><span className="animate-bounce delay-200">.</span></span>
            </div>
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <form onSubmit={handleSend} className="p-4 border-t border-gray-800 bg-black/40 backdrop-blur flex gap-3 relative z-20">
        <Input
          value={input}
          onChange={e => setInput(e.target.value)}
          placeholder="Ingresa tu consulta sobre el documento..."
          disabled={loading}
          className="flex-1 bg-black/50 border-gray-700 focus:border-primary placeholder-gray-600"
        />
        <Button type="submit" disabled={loading} className="px-4 shadow-[0_0_15px_rgba(0,255,65,0.1)]">
          {loading ? <Loader2 className="animate-spin" size={20} /> : <Send size={20} />}
        </Button>
      </form>
    </div>
  );
};
