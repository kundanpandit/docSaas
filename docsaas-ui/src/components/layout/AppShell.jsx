import AuroraBackground from "@/components/effects/AuroraBackground";

/** Workspace frame: padded on every side so panels float instead of touching the screen edges. */
const AppShell = ({ children }) => {
  return (
    <div className="relative mx-auto flex min-h-screen w-full max-w-[1400px] gap-4 p-3 sm:p-4">
      <AuroraBackground />
      {children}
    </div>
  );
};

export default AppShell;
