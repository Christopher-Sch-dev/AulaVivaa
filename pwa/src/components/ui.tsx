import React from 'react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export const Button = ({ className, variant = 'primary', ...props }: React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'ghost' | 'danger' }) => {
  const variants = {
    // Primary: Strong Violet background, sharp text
    primary: "bg-[#6D28D9] hover:bg-[#5B21B6] text-white shadow-[0_0_15px_rgba(109,40,217,0.4)] border border-white/10",

    // Ghost: Transparent but with HOVER glow, not static white wash
    ghost: "bg-transparent text-gray-300 border border-transparent hover:bg-white/5 hover:text-white hover:border-white/10",

    danger: "bg-red-900/20 border border-red-500/30 text-red-400 hover:bg-red-900/40"
  };

  return (
    <button
      className={cn(
        "px-5 py-2.5 rounded-lg font-medium tracking-wide transition-all duration-200 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed select-none",
        variants[variant],
        className
      )}
      {...props}
    />
  );
};

export const Input = ({ className, ...props }: React.InputHTMLAttributes<HTMLInputElement>) => (
  <input
    className={cn(
      // Dark background (black/50) instead of surface/50 to ensure it sits "in" the card
      "w-full bg-black/40 border border-white/10 rounded-lg px-4 py-3 focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary/50 transition-all text-white placeholder-gray-600 font-sans",
      className
    )}
    {...props}
  />
);

export const Card = ({ className, children, ...props }: React.HTMLAttributes<HTMLDivElement>) => (
  // Dark Glass: Black base with slight blur. NO white tint.
  <div
    className={cn(
      "bg-[#0F0E16]/95 backdrop-blur-md border border-white/10 rounded-xl p-6 hover:border-primary/30 transition-all duration-300 shadow-xl",
      className
    )}
    {...props}
  >
    {children}
  </div>
);
