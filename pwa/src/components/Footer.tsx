
import { ExternalLink, Code2 } from 'lucide-react';

export const Footer = () => {
    return (
        <footer className="w-full py-6 mt-12 border-t border-white/5 bg-background/50 backdrop-blur-sm">
            <div className="max-w-7xl mx-auto px-4 flex flex-col md:flex-row items-center justify-between gap-4 text-sm text-gray-500">

                <div className="flex items-center gap-2">
                    <Code2 size={16} className="text-primary/60" />
                    <span>
                        {new Date().getFullYear()} Aula Viva AI <span className="mx-2">&bull;</span> v2.0 PWA
                    </span>
                </div>

                <a
                    href="https://portafolio-devchris.vercel.app/"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="group flex items-center gap-2 px-4 py-2 rounded-full bg-white/5 hover:bg-primary/10 border border-white/5 hover:border-primary/30 transition-all duration-300"
                >
                    <span className="group-hover:text-primary transition-colors">Desarrollado por</span>
                    <span className="font-bold text-gray-300 group-hover:text-white transition-colors">Christopher Schiefelbein</span>
                    <ExternalLink size={14} className="opacity-0 -ml-2 group-hover:opacity-100 group-hover:ml-0 transition-all text-primary" />
                </a>

            </div>
        </footer>
    );
};
