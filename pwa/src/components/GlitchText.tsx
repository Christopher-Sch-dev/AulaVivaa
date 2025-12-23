
import { twMerge } from 'tailwind-merge';

export const GlitchText = ({ text, className }: { text: string, className?: string }) => {
  return (
    <div className={twMerge("relative inline-block", className)}>
      <span className="relative z-10">{text}</span>
      <span className="absolute top-0 left-0 -z-10 w-full h-full text-primary opacity-70 animate-glitch-1 clip-path-polygon-1">
        {text}
      </span>
      <span className="absolute top-0 left-0 -z-10 w-full h-full text-accent opacity-70 animate-glitch-2 clip-path-polygon-2">
        {text}
      </span>
    </div>
  );
};
