import { Card, Button } from '../components/ui';
import { GlitchText } from '../components/GlitchText';
import { BrainCircuit, WifiOff, ExternalLink, GraduationCap, Code2, Server, ShieldCheck, FileText, History } from 'lucide-react';

export const AboutPage = () => {
    return (
        <div className="space-y-12 animate-in fade-in duration-700">

            {/* Header Section */}
            <div className="text-center space-y-6">
                <div className="inline-block p-3 bg-primary/10 rounded-full border border-primary/20 mb-4">
                    <GraduationCap size={48} className="text-primary" />
                </div>
                <GlitchText text="ACERCA DEL PROYECTO" className="text-4xl md:text-5xl font-bold text-white tracking-widest" />
                <p className="text-xl text-gray-400 max-w-2xl mx-auto leading-relaxed">
                    Proyecto académico semestral de Ingeniería en Informática, diseñado para revolucionar la tutoría personalizada mediante IA Offline.
                </p>
            </div>

            {/* Main Info Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8 items-start">

                {/* Contexto */}
                <Card className="hover:border-primary/50 transition-colors">
                    <div className="flex items-start gap-4">
                        <div className="p-3 bg-white/5 rounded-lg text-secondary">
                            <Code2 size={24} />
                        </div>
                        <div className="space-y-3">
                            <h3 className="text-xl font-bold text-white">Contexto Académico</h3>
                            <p className="text-gray-400 leading-relaxed">
                                Desarrollado como proyecto semestral en <strong>Duoc UC</strong> por Christopher Schiefelbein, estudiante de Ingeniería en Informática (a 2 años de titulación).
                            </p>
                            <p className="text-gray-400 leading-relaxed">
                                Este sistema es un <strong>Port Web (PWA)</strong> completo de la aplicación nativa Android "Aula Viva" antes creada, demostrando dominio en arquitecturas multiplataforma.
                            </p>
                        </div>
                    </div>
                </Card>

                {/* Problema y Solución */}
                <Card className="hover:border-primary/50 transition-colors">
                    <div className="flex flex-col gap-4">
                        <div className="bg-black/40 backdrop-blur-md p-6 rounded-2xl border border-white/5 hover:border-primary/30 transition-all group">
                            <div className="w-12 h-12 rounded-full bg-primary/10 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
                                <FileText size={24} className="text-primary" />
                            </div>
                            <h3 className="text-xl font-bold text-white mb-2">Motor Neuronal Híbrido</h3>
                            <p className="text-gray-400 text-sm leading-relaxed">
                                Sistema de ingestión de documentos "God Mode". Combina reconstrucción de layout inteligente con
                                <span className="text-secondary font-bold"> OCR (Visión Artificial)</span> en cliente. Lee desde PDFs nativos hasta escaneos antiguos.
                            </p>
                        </div>

                        <div className="bg-black/40 backdrop-blur-md p-6 rounded-2xl border border-white/5 hover:border-primary/30 transition-all group">
                            <div className="w-12 h-12 rounded-full bg-secondary/10 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
                                <History size={24} className="text-secondary" />
                            </div>
                            <h3 className="text-xl font-bold text-white mb-2">Memoria Contextual RAG</h3>
                            <p className="text-gray-400 text-sm leading-relaxed">
                                Transformamos el modelo stateless en una sesión persistente. El asistente "recuerda" toda la conversación y mantiene el contexto del documento activo, permitiendo preguntas de seguimiento complejas.
                            </p>
                        </div>

                        <div className="bg-black/40 backdrop-blur-md p-6 rounded-2xl border border-white/5 hover:border-primary/30 transition-all group">
                            <div className="w-12 h-12 rounded-full bg-purple-500/10 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
                                <BrainCircuit size={24} className="text-purple-400" />
                            </div>
                            <h3 className="text-xl font-bold text-white mb-2">Arquitectura & Demo</h3>
                            <p className="text-gray-400 leading-relaxed">
                                Para efectos de este <strong>Portafolio/Demo</strong>, se optó por una arquitectura <em>"Local-Only"</em> para garantizar accesibilidad total.
                            </p>
                            <div className="bg-primary/5 border-l-2 border-primary p-3 rounded-r-lg my-2">
                                <p className="text-xs text-gray-300 italic">
                                    * Nota: En producción, la persistencia usaría una BD centralizada (Supabase) para integridad de datos, lógica que domino (Spring Boot) pero omití aquí por portabilidad.
                                </p>
                            </div>
                            <ul className="space-y-2 text-sm text-gray-400 mt-2">
                                <li className="flex items-center gap-2">
                                    <WifiOff size={16} className="text-red-400" />
                                    <span>Funcionamiento 100% Offline (IndexedDB)</span>
                                </li>
                                <li className="flex items-center gap-2">
                                    <ShieldCheck size={16} className="text-green-400" />
                                    <span>Privacidad P2P para IA y Documentos</span>
                                </li>
                            </ul>
                        </div>
                    </div>
                </Card>

            </div>

            {/* Stack Técnico */}
            <div>
                <h2 className="text-2xl font-bold text-white mb-6 flex items-center gap-3">
                    <Server className="text-secondary" /> Evolución del Stack
                </h2>

                {/* Comparison Grid */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
                    {/* Android Native */}
                    <div className="bg-white/5 border border-white/5 rounded-xl p-6 relative overflow-hidden group hover:border-white/10 transition-colors">
                        <div className="absolute top-0 left-0 w-1 h-full bg-green-500/50" />
                        <div className="flex items-center gap-3 mb-4">
                            <div className="p-2 bg-green-500/10 rounded-lg">
                                <Code2 size={20} className="text-green-400" />
                            </div>
                            <h3 className="font-bold text-white">Versión Nativa (Original)</h3>
                        </div>
                        <ul className="space-y-3 text-sm text-gray-400">
                            <li className="flex items-center justify-between border-b border-white/5 pb-2">
                                <span>Plataforma</span> <span className="text-green-400 font-mono">Android (APK)</span>
                            </li>
                            <li className="flex items-center justify-between border-b border-white/5 pb-2">
                                <span>Lenguaje</span> <span className="text-gray-300">Kotlin + Compose</span>
                            </li>
                            <li className="flex items-center justify-between border-b border-white/5 pb-2">
                                <span>Backend</span> <span className="text-gray-300">Spring Boot (Java)</span>
                            </li>
                            <li className="flex items-center justify-between border-b border-white/5 pb-2">
                                <span>BD</span> <span className="text-gray-300">Supabase + Room</span>
                            </li>
                            <li className="mt-2 text-xs italic opacity-70">
                                "Robusta pero requiere instalación."
                            </li>
                        </ul>
                    </div>

                    {/* PWA Port */}
                    <div className="bg-primary/5 border border-primary/20 rounded-xl p-6 relative overflow-hidden">
                        <div className="absolute top-0 left-0 w-1 h-full bg-primary" />
                        <div className="flex items-center gap-3 mb-4">
                            <div className="p-2 bg-primary/20 rounded-lg">
                                <BrainCircuit size={20} className="text-primary" />
                            </div>
                            <h3 className="font-bold text-white">Versión PWA (Demo)</h3>
                        </div>
                        <ul className="space-y-3 text-sm text-gray-300">
                            <li className="flex items-center justify-between border-b border-white/5 pb-2">
                                <span>Plataforma</span> <span className="text-primary font-bold font-mono">Web Universal</span>
                            </li>
                            <li className="flex items-center justify-between border-b border-white/5 pb-2">
                                <span>Lenguaje</span> <span>React + TypeScript</span>
                            </li>
                            <li className="flex items-center justify-between border-b border-white/5 pb-2">
                                <span>Backend</span> <span>Client-Side (Edge)</span>
                            </li>
                            <li className="flex items-center justify-between border-b border-white/5 pb-2">
                                <span>BD</span> <span>Dexie.js (NoSQL)</span>
                            </li>
                            <li className="mt-2 text-xs text-primary italic">
                                "Acceso instantáneo para demostración."
                            </li>
                        </ul>
                    </div>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
                    {[
                        { name: "React 18", desc: "Framework Principal" },
                        { name: "TypeScript", desc: "Tipado Seguro" },
                        { name: "Vite", desc: "Empaquetador Rápido" },
                        { name: "Tailwind CSS", desc: "Motor de Estilos" },
                        { name: "Dexie.js", desc: "Base de Datos Local" },
                        { name: "Google Gemini", desc: "IA Generativa" },
                        { name: "Framer Motion", desc: "Animaciones UI" },
                        { name: "PWA", desc: "Capacidad Offline" }
                    ].map((tech, i) => (
                        <div key={i} className="bg-white/5 border border-white/5 rounded-lg p-4 hover:bg-white/10 transition-colors">
                            <h4 className="font-bold text-primary">{tech.name}</h4>
                            <p className="text-xs text-gray-500 mt-1">{tech.desc}</p>
                        </div>
                    ))}
                </div>
            </div>

            {/* Impacto */}
            <Card className="bg-linear-to-br from-primary/10 to-transparent border-primary/20">
                <div className="text-center space-y-4">
                    <h3 className="text-2xl font-bold text-white">Amplificador de Aprendizaje</h3>
                    <p className="text-gray-300 max-w-3xl mx-auto leading-relaxed italic">
                        "Tu tutor de bolsillo: personalización real para cada asignatura."
                    </p>
                    <p className="text-gray-400 max-w-3xl mx-auto leading-relaxed mt-4">
                        Aula Viva actúa como un <strong>tutor cognitivo</strong>. No solo gestiona contenido, sino que lo <em>moldea</em> para adaptarse al estilo de cada estudiante o docente; potenciando la metodologia del aprendizaje.
                        Es un <strong>Tutor de Bolsillo</strong> que acompaña el proceso educativo, acelerando la curva de aprendizaje y asegurando que la enseñanza sea precisa, contextual y profundamente efectiva.
                    </p>
                    <div className="pt-4">
                        <Button onClick={() => window.open('https://portafolio-devchris.vercel.app/', '_blank')} className="gap-2">
                            Ver Portafolio Profesional <ExternalLink size={16} />
                        </Button>
                    </div>
                </div>
            </Card>

            <div className="h-12" /> {/* Bottom Spacer */}
        </div>
    );
};


