
import { Card } from './ui';
import { type LucideIcon } from 'lucide-react';

interface StatCardProps {
    label: string;
    value: string | number;
    icon: LucideIcon;
    trend?: string;
    color?: string; // Hex color for the glow
    delay?: number; // Animation delay index
}

export const StatCard = ({ label, value, icon: Icon, trend, color = '#7C3AED', delay = 0 }: StatCardProps) => {
    return (
        <Card className="relative overflow-hidden group hover:border-primary/50 transition-all duration-500 animate-in fade-in zoom-in" style={{ animationDelay: `${delay * 100}ms` }}>
            {/* Background Glow */}
            <div
                className="absolute -right-6 -top-6 w-24 h-24 rounded-full opacity-10 blur-2xl transition-all group-hover:scale-150 duration-500"
                style={{ backgroundColor: color }}
            />

            <div className="relative z-10 flex items-start justify-between">
                <div>
                    <p className="text-sm font-medium text-gray-400 mb-1">{label}</p>
                    <h3 className="text-3xl font-bold text-white tracking-tight">{value}</h3>

                    {trend && (
                        <p className="text-xs mt-2 text-emerald-400 flex items-center gap-1 font-mono">
                            {trend}
                        </p>
                    )}
                </div>

                <div
                    className="p-3 rounded-xl bg-white/5 border border-white/10 group-hover:scale-110 transition-transform duration-300"
                >
                    <Icon size={24} style={{ color }} />
                </div>
            </div>
        </Card>
    );
};
