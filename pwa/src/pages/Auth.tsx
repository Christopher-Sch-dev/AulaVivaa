import React, { useState, useEffect } from 'react';
import { useStore } from '../store/useStore';
import { AuthService } from '../services/auth';
import { Button, Input, Card } from '../components/ui';
import { useNavigate } from 'react-router-dom';

import { motion, AnimatePresence } from 'framer-motion';
import { toast } from 'sonner';
import { BrainCircuit, WifiOff, ShieldCheck, GraduationCap, ArrowRight, Rocket } from 'lucide-react';

const FEATURES = [
  {
    icon: BrainCircuit,
    title: "Inteligencia Artificial Educativa",
    desc: "Gemini 3 Flash analiza tus guías PDF y responde dudas al instante, adaptándose a tu ritmo de aprendizaje."
  },
  {
    icon: WifiOff,
    title: "Tecnología Offline-First",
    desc: "Accede a tus clases, documentos y chats sin conexión a internet. Sincronización automática al reconectar."
  },
  {
    icon: ShieldCheck,
    title: "Seguridad Privada",
    desc: "Tus datos y claves API residen en tu dispositivo. Comunicación encriptada punto a punto."
  }
];

export const AuthPage = () => {
  const [isLogin, setIsLogin] = useState(true);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [role, setRole] = useState<'docente' | 'alumno'>('alumno');
  const [loading, setLoading] = useState(false);
  const [featureIndex, setFeatureIndex] = useState(0);

  const { setUser } = useStore();
  const navigate = useNavigate();

  // Feature Carousel
  useEffect(() => {
    const timer = setInterval(() => {
      setFeatureIndex(prev => (prev + 1) % FEATURES.length);
    }, 5000);
    return () => clearInterval(timer);
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    // Simulator delay for "Processing" feel
    await new Promise(r => setTimeout(r, 800));

    try {
      if (isLogin) {
        const user = await AuthService.login(email, password);
        setUser(user);
        toast.success(`Bienvenido, ${user.name}`);
        navigate('/');
      } else {
        const user = await AuthService.register({ email, passwordHash: password, name, role });
        setUser(user);
        toast.success('Cuenta creada exitosamente');
        navigate('/');
      }
    } catch (err: any) {
      toast.error(err.message || 'Error de autenticación');
      const form = document.getElementById('auth-form');
      form?.animate([
        { transform: 'translateX(0)' },
        { transform: 'translateX(-10px)' },
        { transform: 'translateX(10px)' },
        { transform: 'translateX(0)' }
      ], { duration: 300 });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-[80vh] w-full mt-[-40px]">
      <div className="w-full max-w-6xl grid grid-cols-1 lg:grid-cols-2 gap-12 lg:gap-24 items-center">

        {/* Left Column: Educational Showcase */}
        <div className="hidden lg:flex flex-col justify-center space-y-12">
          <div>
            <div className="flex items-center gap-3 mb-4">
              <GraduationCap size={40} className="text-secondary" />
              <span className="px-3 py-1 rounded-full border border-secondary/30 bg-secondary/10 text-secondary text-xs uppercase font-bold tracking-widest">
                Versión 2.0 PWA
              </span>
            </div>
            <h1 className="text-5xl font-bold text-white leading-tight mb-6">
              La Evolución del <br />
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-primary to-secondary">Aprendizaje Digital</span>
            </h1>
            <p className="text-lg text-gray-400 max-w-md leading-relaxed">
              Aula Viva conecta docentes y estudiantes con el poder de la IA Generativa, en una plataforma segura y siempre disponible.
            </p>
          </div>

          {/* Feature Card Carousel */}
          <div className="relative h-48">
            <AnimatePresence mode="wait">
              <motion.div
                key={featureIndex}
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -20 }}
                transition={{ duration: 0.5 }}
                className="bg-surface/30 border border-white/5 backdrop-blur-sm p-6 rounded-2xl flex items-start gap-4 absolute w-full"
              >
                <div className="p-3 bg-primary/20 rounded-lg text-primary">
                  {React.createElement(FEATURES[featureIndex].icon, { size: 28 })}
                </div>
                <div>
                  <h3 className="text-xl font-bold text-white mb-2">{FEATURES[featureIndex].title}</h3>
                  <p className="text-sm text-gray-400 leading-relaxed">{FEATURES[featureIndex].desc}</p>
                </div>
              </motion.div>
            </AnimatePresence>

            {/* Indicators */}
            <div className="absolute -bottom-8 flex gap-2">
              {FEATURES.map((_, i) => (
                <div
                  key={i}
                  className={`h-1.5 rounded-full transition-all duration-300 ${i === featureIndex ? 'w-8 bg-primary' : 'w-2 bg-gray-700'}`}
                />
              ))}
            </div>
          </div>
        </div>

        {/* Right Column: Login Form */}
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.2 }}
        >
          <Card id="auth-form" className="border-white/10 bg-[#0F0E16]/90 backdrop-blur-xl shadow-2xl relative overflow-hidden">
            {/* Decorative top line */}
            <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-primary via-secondary to-primary opacity-80" />

            <div className="text-center mb-8">
              {/* Demo Disclaimer */}
              <div className="flex items-center justify-center gap-2 text-primary mb-1">
                <Rocket size={12} />
                <p className="text-[10px] font-bold tracking-wider uppercase">Demo Funcional Completa</p>
              </div>
              <p className="text-[10px] text-gray-400">Port from Kotlin Android Academy App</p>


              <h2 className="text-2xl font-bold text-white mb-2">{isLogin ? 'Iniciar Sesión' : 'Crear Cuenta'}</h2>
              <p className="text-sm text-gray-400">
                {isLogin ? 'Accede a tu cuenta institucional' : 'Únete a la comunidad de Aula Viva'}
              </p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-5">
              {!isLogin && (
                <div className="space-y-1">
                  <label className="text-xs text-gray-500 font-bold ml-1">NOMBRE COMPLETO</label>
                  <Input
                    placeholder="Ej: Juan Pérez"
                    value={name}
                    onChange={e => setName(e.target.value)}
                    required
                  />
                </div>
              )}

              <div className="space-y-1">
                <label className="text-xs text-gray-500 font-bold ml-1">CORREO INSTITUCIONAL</label>
                <Input
                  type="email"
                  placeholder="usuario@duoc.cl"
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  required
                />
              </div>

              <div className="space-y-1">
                <label className="text-xs text-gray-500 font-bold ml-1">CONTRASEÑA</label>
                <Input
                  type="password"
                  placeholder="••••••••"
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  required
                />
              </div>

              {!isLogin && (
                <div className="grid grid-cols-2 gap-3 pt-2">
                  <label className={`cursor-pointer border rounded-lg p-3 text-center transition-all ${role === 'alumno' ? 'border-primary bg-primary/10 text-white' : 'border-white/10 text-gray-500 hover:border-white/30'}`}>
                    <input type="radio" className="hidden" name="role" checked={role === 'alumno'} onChange={() => setRole('alumno')} />
                    <span className="text-sm font-bold">Soy Alumno</span>
                  </label>
                  <label className={`cursor-pointer border rounded-lg p-3 text-center transition-all ${role === 'docente' ? 'border-primary bg-primary/10 text-white' : 'border-white/10 text-gray-500 hover:border-white/30'}`}>
                    <input type="radio" className="hidden" name="role" checked={role === 'docente'} onChange={() => setRole('docente')} />
                    <span className="text-sm font-bold">Soy Docente</span>
                  </label>
                </div>
              )}

              <Button
                type="submit"
                disabled={loading}
                className="w-full mt-6 py-3 text-base flex justify-center items-center gap-2"
              >
                {loading ? 'Procesando...' : (isLogin ? 'Ingresar a la Plataforma' : 'Registrar Usuario')}
                {!loading && <ArrowRight size={18} />}
              </Button>
            </form>

            <div className="mt-6 text-center pt-6 border-t border-white/5">
              <button
                type="button"
                onClick={() => setIsLogin(!isLogin)}
                className="text-sm text-secondary hover:text-white transition-colors"
              >
                {isLogin ? '¿No tienes cuenta? Regístrate aquí' : '¿Ya tienes cuenta? Inicia sesión'}
              </button>
            </div>
          </Card>
        </motion.div>
      </div>
    </div >
  );
};
