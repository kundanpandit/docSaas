const SectionDivider = () => {
  return (
    <div className="relative ">
      <div className="absolute left-0 right-0 top-1/2 h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent" />
      <div className="relative mx-auto flex h-12 w-12 items-center justify-center rounded-full border border-primary/20 bg-background backdrop-blur-xl">
        <div className="h-2 w-2 rounded-full bg-primary animate-pulse" />
      </div>
    </div>
  );
};

export default SectionDivider;