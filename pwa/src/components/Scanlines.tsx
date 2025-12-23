export const Scanlines = () => (
  <div className="pointer-events-none fixed inset-0 z-50 overflow-hidden opacity-[0.04]">
    {/* Increased opacity from 0.02 to 0.04 for better visibility */}
    <div className="h-full w-full bg-[linear-gradient(rgba(18,16,16,0)_50%,rgba(0,0,0,0.25)_50%),linear-gradient(90deg,rgba(255,0,0,0.06),rgba(0,255,0,0.02),rgba(0,0,255,0.06))] bg-[length:100%_2px,3px_100%] bg-repeat" />
  </div>
);
