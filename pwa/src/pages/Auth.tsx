import React, { useState } from 'react';
import { useStore } from '../store/useStore';
import { AuthService } from '../services/auth';
import { Button, Input, Card } from '../components/ui';
import { useNavigate } from 'react-router-dom';
import { GlitchText } from '../components/GlitchText';
import { motion } from 'framer-motion';

export const AuthPage = () => {
  const [isLogin, setIsLogin] = useState(true);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [role, setRole] = useState<'docente' | 'alumno'>('alumno');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  
  const { setUser } = useStore();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      if (isLogin) {
        const user = await AuthService.login(email, password);
        setUser(user);
        navigate('/');
      } else {
        const user = await AuthService.register({ email, passwordHash: password, name, role });
        setUser(user);
        navigate('/');
      }
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex justify-center items-center min-h-[80vh] px-4">
      <motion.div 
        initial={{ scale: 0.9, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ duration: 0.5 }}
        className="w-full max-w-md"
      >
        <Card className="border-primary/20 shadow-[0_0_80px_rgba(0,255,65,0.05)]">
          <div className="text-center mb-8">
            <GlitchText 
                text={isLogin ? 'ACCESO AL SISTEMA' : 'REGISTRO DE USUARIO'} 
                className="text-3xl font-bold text-primary"
            />
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            {!isLogin && (
              <Input 
                placeholder="Nombre Completo" 
                value={name} 
                onChange={e => setName(e.target.value)} 
                required 
              />
            )}
            
            <Input 
              type="email" 
              placeholder="Correo Electrónico" 
              value={email} 
              onChange={e => setEmail(e.target.value)} 
              required 
            />
            
            <Input 
              type="password" 
              placeholder="Contraseña" 
              value={password} 
              onChange={e => setPassword(e.target.value)} 
              required 
            />

            {!isLogin && (
              <div className="grid grid-cols-2 gap-4">
                 <label className={`flex items-center justify-center gap-2 p-3 rounded border cursor-pointer transition-all ${role === 'alumno' ? 'bg-primary/20 border-primary text-white' : 'border-gray-700 text-gray-400 hover:border-gray-500'}`}>
                   <input 
                     type="radio" 
                     name="role" 
                     value="alumno" 
                     checked={role === 'alumno'} 
                     onChange={() => setRole('alumno')}
                     className="hidden"
                   />
                   ALUMNO
                 </label>
                 <label className={`flex items-center justify-center gap-2 p-3 rounded border cursor-pointer transition-all ${role === 'docente' ? 'bg-primary/20 border-primary text-white' : 'border-gray-700 text-gray-400 hover:border-gray-500'}`}>
                   <input 
                     type="radio" 
                     name="role" 
                     value="docente" 
                     checked={role === 'docente'} 
                     onChange={() => setRole('docente')}
                     className="hidden"
                   />
                   DOCENTE
                 </label>
              </div>
            )}

            {error && <p className="text-accent text-sm text-center font-bold animate-pulse bg-accent/10 py-1 rounded border border-accent/20">{error}</p>}

            <Button type="submit" className="w-full text-lg shadow-[0_0_20px_rgba(0,255,65,0.2)]" disabled={loading}>
              {loading ? 'PROCESANDO...' : (isLogin ? 'INGRESAR' : 'REGISTRARSE')}
            </Button>

            <div className="text-center mt-6">
              <button 
                type="button" 
                onClick={() => setIsLogin(!isLogin)} 
                className="text-sm text-gray-500 hover:text-primary transition-colors hover:underline underline-offset-4"
              >
                {isLogin ? '¿No tienes cuenta? CREAR CUENTA' : '¿Ya tienes cuenta? INICIAR SESIÓN'}
              </button>
            </div>
          </form>
        </Card>
      </motion.div>
    </div>
  );
};
