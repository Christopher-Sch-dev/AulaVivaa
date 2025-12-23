import React from 'react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export const Button = ({ className, variant = 'primary', ...props }: React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'ghost' | 'danger' }) => {
  const variants = {
    primary: "bg-primary text-black hover:bg-secondary hover:shadow-[0_0_15px_rgba(0,255,65,0.5)] border border-transparent",
    ghost: "bg-transparent border border-primary/30 text-primary hover:bg-primary/10 hover:border-primary",
    danger: "bg-accent/10 border border-accent text-accent hover:bg-accent hover:text-white"
  };

  return (
    <button 
      className={cn(
        "px-4 py-2 rounded font-bold transition-all duration-300 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed uppercase tracking-wider text-sm",
        variants[variant],
        className
      )} 
      {...props} 
    />
  );
};

export const Input = ({ className, ...props }: React.InputHTMLAttributes<HTMLInputElement>) => (
  <div className="relative group">
     <div className="absolute inset-0 bg-primary/20 blur-md opacity-0 group-focus-within:opacity-100 transition-opacity rounded" />
     <input 
        className={cn(
        "relative w-full bg-surface/50 border border-gray-700 rounded px-4 py-3 focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-all text-white placeholder-gray-500 font-mono",
        className
        )} 
        {...props} 
    />
  </div>
);

export const Card = ({ className, children, ...props }: React.HTMLAttributes<HTMLDivElement>) => (
  <div 
    className={cn(
      "bg-black/60 backdrop-blur-md border border-gray-800 rounded-lg p-6 hover:border-primary/50 transition-all duration-300 shadow-[0_4px_20px_rgba(0,0,0,0.5)]",
      className
    )} 
    {...props}
  >
    {children}
  </div>
);
