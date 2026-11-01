/** Fixed, decorative background: slow-moving color blobs behind a faint grid. */
const AuroraBackground = () => (
  <div aria-hidden="true" className="pointer-events-none fixed inset-0 -z-10 overflow-hidden">
    <div className="absolute inset-0 bg-grid" />
    <div
      className="animate-aurora absolute -top-[20%] left-[5%] h-[55vh] w-[55vw] rounded-full blur-[110px]"
      style={{ background: "var(--aurora-1)" }}
    />
    <div
      className="animate-aurora absolute top-[10%] -right-[10%] h-[45vh] w-[45vw] rounded-full blur-[110px]"
      style={{ background: "var(--aurora-2)", animationDelay: "-8s" }}
    />
    <div
      className="animate-aurora absolute top-[55%] left-[30%] h-[40vh] w-[40vw] rounded-full blur-[120px]"
      style={{ background: "var(--aurora-3)", animationDelay: "-16s" }}
    />
  </div>
);

export default AuroraBackground;
