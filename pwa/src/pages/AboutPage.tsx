import { Card, Button } from '../components/ui';
import { GlitchText } from '../components/GlitchText';
import { BrainCircuit, WifiOff, ExternalLink, GraduationCap, Code2, Server, ShieldCheck } from 'lucide-react';

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
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">

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
                                Este sistema es un <strong>Port Web (PWA)</strong> completo de la aplicación nativa Android "Aula Viva", demostrando dominio en arquitecturas multiplataforma.
                            </p>
                        </div>
                    </div>
                </Card>

                {/* Problema y Solución */}
                <Card className="hover:border-primary/50 transition-colors">
                    <div className="flex items-start gap-4">
                        <div className="p-3 bg-white/5 rounded-lg text-primary">
                            <BrainCircuit size={24} />
                        </div>
                        <div className="space-y-3">
                            <h3 className="text-xl font-bold text-white">El Desafío Técnico</h3>
                            <p className="text-gray-400 leading-relaxed">
                                El objetivo fue democratizar el acceso a tutores inteligentes en zonas con baja conectividad.
                            </p>
                            <ul className="space-y-2 text-sm text-gray-400">
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
                    <Server className="text-secondary" /> Stack Tecnológico
                </h2>
                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
                    {[
                        { name: "React 18", desc: "Core Framework" },
                        { name: "TypeScript", desc: "Type Safety" },
                        { name: "Vite", desc: "Build Tool" },
                        { name: "Tailwind CSS", desc: "Styling Engine" },
                        { name: "Dexie.js", desc: "Local Database" },
                        { name: "Google Gemini", desc: "Generative AI" },
                        { name: "Framer Motion", desc: "Animations" },
                        { name: "PWA", desc: "Offline Capabilities" }
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
                    <h3 className="text-2xl font-bold text-white">Impacto y Proyección</h3>
                    <p className="text-gray-300 max-w-3xl mx-auto leading-relaxed">
                        Aula Viva demuestra que es posible llevar herramientas educativas de vanguardia (IA Generativa) al borde (Edge), eliminando barreras de infraestructura y costos de servidor, ideal para implementaciones en instituciones educativas reales.
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


