import React from 'react';
import { MatrixBackground } from './MatrixBackground';
import { Footer } from './Footer';
import { Scanlines } from './Scanlines';
import { useStore } from '../store/useStore';
import { LogOut, User as UserIcon } from 'lucide-react';
import { Toaster } from 'sonner';
import { GlitchText } from './GlitchText';
import { motion, AnimatePresence } from 'framer-motion';
import { useLocation } from 'react-router-dom';

export const Layout = ({ children }: { children: React.ReactNode }) => {
  const { user, logout } = useStore();
  const location = useLocation();

  return (
    <div className="h-screen text-text relative overflow-hidden font-sans selection:bg-primary selection:text-white flex flex-col">
      <MatrixBackground />
      <Scanlines />

      {/* Updated Toaster Theme: Deep Violet */}
      <Toaster
        theme="dark"
        position="top-right"
        toastOptions={{
          style: {
            background: 'rgba(21, 19, 30, 0.95)',
            border: '1px solid #2e2a3d',
            color: '#fff',
            fontFamily: 'Inter, sans-serif'
          }
        }}
      />

      <nav className="border-b border-white/5 bg-background/80 backdrop-blur-md sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16 items-center">
            <div className="flex items-center">
              <GlitchText text="AULA_VIVA_AI" className="text-2xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-primary to-secondary tracking-tighter" />
            </div>
            {user && (
              <div className="flex items-center gap-4">
                <div className="hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-full bg-white/5 border border-white/5">
                  <UserIcon size={14} className="text-secondary" />
                  <span className="text-sm text-gray-300 font-medium">
                    {user.name}
                  </span>
                  <span className="text-[10px] uppercase font-bold text-primary bg-primary/10 px-2 rounded">
                    {user.role}
                  </span>
                </div>
                <button
                  onClick={logout}
                  className="p-2 text-gray-400 hover:text-white hover:bg-white/5 rounded-full transition-all"
                  title="Cerrar Sesión"
                >
                  <LogOut size={20} />
                </button>
              </div>
            )}
          </div>
        </div>
      </nav>

      <main className="flex-1 w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 relative z-10 overflow-y-auto">
        <AnimatePresence mode="wait">
          <motion.div
            key={location.pathname}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.3, ease: 'easeOut' }}
          >
            {children}
          </motion.div>
        </AnimatePresence>
      </main>

      <Footer />
    </div>
  );
};
