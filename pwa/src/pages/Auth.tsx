import React, { useState, useEffect } from 'react';
import { useStore } from '../store/useStore';
import { AuthService } from '../services/auth';
import { Button, Input, Card, PasswordInput } from '../components/ui';
import { useNavigate } from 'react-router-dom';

import { motion, AnimatePresence } from 'framer-motion';
import { toast } from 'sonner';
import { BrainCircuit, WifiOff, ShieldCheck, ArrowRight, Rocket } from 'lucide-react';

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

  const { user, setUser } = useStore();
  const navigate = useNavigate();

  // Redirect if already logged in
  useEffect(() => {
    if (user) {
      navigate('/', { replace: true });
    }
  }, [user, navigate]);

  // Feature Carousel
  useEffect(() => {
    const timer = setInterval(() => {
      setFeatureIndex(prev => (prev + 1) % FEATURES.length);
    }, 5000);
    return () => clearInterval(timer);
  }, [featureIndex]); // Dependency added to reset timer on manual interaction

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    // Validation Logic
    if (!email.includes('@') || email.length < 5) {
      toast.error('Ingresa un correo válido');
      setLoading(false);
      return;
    }
    if (password.length < 6) {
      toast.error('La contraseña debe tener al menos 6 caracteres');
      setLoading(false);
      return;
    }
    if (!isLogin && name.length < 3) {
      toast.error('Ingresa tu nombre completo');
      setLoading(false);
      return;
    }

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
    <div className="flex items-center justify-center min-h-[60vh] w-full mt-[-40px]">
      <div className="w-full max-w-6xl grid grid-cols-1 lg:grid-cols-2 gap-8 lg:gap-16 items-center">

        {/* Left Column: Educational Showcase */}
        <div className="hidden lg:flex flex-col justify-center space-y-6">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/5 border border-white/10 backdrop-blur-md animate-fade-in-up delay-100 mb-4">
              <span className="w-2 h-2 rounded-full bg-secondary animate-pulse" />
              <span className="text-xs font-medium text-gray-300 tracking-wider">POWERED BY GEMINI 3 FLASH + VISION API</span>
            </div>
            <h1 className="text-4xl font-bold text-white leading-tight mb-3">
              La Evolución del <br />
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-primary to-secondary">Aprendizaje Digital</span>
            </h1>
            <p className="text-base text-gray-400 max-w-md leading-relaxed">
              Aula Viva conecta docentes y estudiantes con el poder de la IA Generativa, en una plataforma segura y siempre disponible.
            </p>
          </div>

          {/* Feature Card Carousel */}
          <div className="relative h-32">
            <AnimatePresence mode="wait">
              <motion.div
                key={featureIndex}
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -20 }}
                transition={{ duration: 0.5 }}
                className="bg-surface/30 border border-white/5 backdrop-blur-sm p-4 rounded-2xl flex items-start gap-4 absolute w-full"
              >
                <div className="p-2 bg-primary/20 rounded-lg text-primary">
                  {React.createElement(FEATURES[featureIndex].icon, { size: 24 })}
                </div>
                <div>
                  <h3 className="text-lg font-bold text-white mb-1">{FEATURES[featureIndex].title}</h3>
                  <p className="text-xs text-gray-400 leading-relaxed">{FEATURES[featureIndex].desc}</p>
                </div>
              </motion.div>
            </AnimatePresence>

            {/* Indicators */}
            <div className="absolute -bottom-6 flex gap-2 cursor-pointer z-10">
              {FEATURES.map((_, i) => (
                <button
                  key={i}
                  onClick={() => {
                    setFeatureIndex(i);
                  }}
                  className={`h-1.5 rounded-full transition-all duration-300 ${i === featureIndex ? 'w-6 bg-primary' : 'w-2 bg-gray-700 hover:bg-gray-600'}`}
                />
              ))}
            </div>
          </div>

          {/* Quick Demo Access Card */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.5 }}
            className="bg-black/20 backdrop-blur-xl border border-white/10 rounded-2xl p-5 relative overflow-hidden group hover:border-white/20 transition-colors"
          >
            <div className="absolute top-0 right-0 p-2 opacity-10 group-hover:opacity-20 transition-opacity">
              <Rocket size={48} className="text-white transform rotate-12" />
            </div>

            <h3 className="text-xs font-bold text-gray-400 uppercase tracking-widest mb-3 flex items-center gap-2">
              ⚡ Acceso Rápido (Demo)
            </h3>

            <div className="grid grid-cols-2 gap-3">
              <button
                onClick={() => {
                  setIsLogin(true);
                  setEmail('d1@d1.cl');
                  setPassword('docente12');
                  toast.info('Credenciales de Docente cargadas');
                }}
                className="bg-primary/20 border-2 border-primary/30 hover:bg-primary/30 hover:border-primary/60 transition-all p-3 rounded-xl text-left group/btn flex items-center justify-between shadow-lg shadow-primary/10"
              >
                <div>
                  <div className="text-[10px] text-primary font-bold mb-0.5 uppercase tracking-wider group-hover/btn:translate-x-1 transition-transform">Docente</div>
                  <div className="text-white font-mono text-xs mb-0.5 font-bold">d1@d1.cl</div>
                  <div className="text-gray-400 text-[10px]">Pass: docente12</div>
                </div>
                <div className="bg-primary/20 p-1.5 rounded-full group-hover/btn:bg-primary group-hover/btn:text-white transition-colors">
                  <ArrowRight size={16} className="text-primary group-hover/btn:text-white" />
                </div>
              </button>

              <button
                onClick={() => {
                  setIsLogin(true);
                  setEmail('a1@a1.cl');
                  setPassword('alumno12');
                  toast.info('Credenciales de Alumno cargadas');
                }}
                className="bg-secondary/20 border-2 border-secondary/30 hover:bg-secondary/30 hover:border-secondary/60 transition-all p-3 rounded-xl text-left group/btn flex items-center justify-between shadow-lg shadow-secondary/10"
              >
                <div>
                  <div className="text-[10px] text-secondary font-bold mb-0.5 uppercase tracking-wider group-hover/btn:translate-x-1 transition-transform">Alumno</div>
                  <div className="text-white font-mono text-xs mb-0.5 font-bold">a1@a1.cl</div>
                  <div className="text-gray-400 text-[10px]">Pass: alumno12</div>
                </div>
                <div className="bg-secondary/20 p-1.5 rounded-full group-hover/btn:bg-secondary group-hover/btn:text-white transition-colors">
                  <ArrowRight size={16} className="text-secondary group-hover/btn:text-white" />
                </div>
              </button>
            </div>

            <p className="text-[10px] text-gray-500 mt-3 text-center italic">
              * Haz clic en una tarjeta para auto-completar el formulario
            </p>
          </motion.div>
        </div>
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
              <p className="text-[10px] text-gray-400">Port Web desde App Nativa Android (Kotlin)</p>


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
                <PasswordInput
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
