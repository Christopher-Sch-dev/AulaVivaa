import React from 'react';
import { MatrixBackground } from './MatrixBackground';
import { Scanlines } from './Scanlines';
import { useStore } from '../store/useStore';
import { LogOut } from 'lucide-react';
import { Toaster } from 'sonner';
import { GlitchText } from './GlitchText';
import { motion, AnimatePresence } from 'framer-motion';
import { useLocation } from 'react-router-dom';

export const Layout = ({ children }: { children: React.ReactNode }) => {
  const { user, logout } = useStore();
  const location = useLocation();

  return (
    <div className="min-h-screen text-text relative overflow-hidden font-mono selection:bg-primary selection:text-black">
      <MatrixBackground />
      <Scanlines />
      <Toaster
        theme="dark"
        position="top-right"
        toastOptions={{
          style: {
            background: 'rgba(13,13,13,0.9)',
            border: '1px solid #00FF41',
            color: '#fff',
            fontFamily: 'monospace'
          }
        }}
      />

      <nav className="border-b border-primary/30 bg-background/80 backdrop-blur-sm sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16 items-center">
            <div className="flex items-center">
              <GlitchText text="<AULA_VIVA />" className="text-2xl font-bold text-primary tracking-tighter" />
            </div>
            {user && (
              <div className="flex items-center gap-4">
                <span className="text-sm text-gray-400 hidden sm:inline">
                  {user.name} [{user.role.toUpperCase()}]
                </span>
                <button
                  onClick={logout}
                  className="p-2 hover:text-primary transition-colors border border-transparent hover:border-primary/50 rounded"
                >
                  <LogOut size={20} />
                </button>
              </div>
            )}
          </div>
        </div>
      </nav>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 relative z-10">
        <AnimatePresence mode="wait">
          <motion.div
            key={location.pathname}
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -15 }}
            transition={{ duration: 0.3 }}
          >
            {children}
          </motion.div>
        </AnimatePresence>
      </main>
    </div>
  );
};
