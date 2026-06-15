import {
  Upload,
  MousePointerClick,
  ShieldCheck,
  Download,
  ArrowRight,
} from "lucide-react";

const steps = [
  {
    title: "Upload Document",
    description: "Upload PDF, Word or Image files.",
    icon: Upload,
  },
  {
    title: "Choose Tool",
    description: "Select OCR, Convert, Compress or AI tools.",
    icon: MousePointerClick,
  },
  {
    title: "Process Securely",
    description: "Your files are processed quickly and securely.",
    icon: ShieldCheck,
  },
  {
    title: "Download Result",
    description: "Get your processed file instantly.",
    icon: Download,
  },
];

const HowItWorks = () => {
  return (
    <section className="relative overflow-hidden py-20 md:py-28">
      <div className="absolute inset-0 -z-10 overflow-hidden pointer-events-none">
        <div className="absolute left-[15%] top-[20%] h-[300px] w-[300px] rounded-full bg-primary/5 blur-[120px]" />
        <div className="absolute right-[15%] bottom-[10%] h-[300px] w-[300px] rounded-full bg-violet-500/5 blur-[120px]" />
      </div>

      <div className="container mx-auto px-5 md:px-6">
        <div className="mx-auto mb-16 max-w-3xl text-center">
          <div className="mb-4 inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/5 px-4 py-2 text-sm text-primary">
            How It Works
          </div>

          <h2 className="text-4xl font-bold tracking-tight md:text-5xl">
            Simple
            <span className="bg-linear-to-r from-primary via-purple-400 to-cyan-400 bg-clip-text text-transparent">
              {" "}Workflow
            </span>
          </h2>

          <p className="mt-4 text-lg text-muted-foreground">
            Process your documents in just a few clicks.
          </p>
        </div>
            <div className="relative">
  <div className="hidden lg:block absolute left-[12%] right-[12%] top-[60px] h-px">
    <div className="absolute inset-0 border-t border-dashed border-primary/20" />
    <div className="absolute left-0 top-1/2 h-2 w-2 -translate-y-1/2 rounded-full bg-primary shadow-[0_0_15px_rgba(139,92,246,0.8)] animate-[flow1_8s_linear_infinite]" />

<div className="absolute left-0 top-1/2 h-1.5 w-1.5 -translate-y-1/2 rounded-full bg-purple-400 shadow-[0_0_12px_rgba(168,85,247,0.8)] animate-[flow2_8s_linear_infinite_2s]" />

<div className="absolute left-0 top-1/2 h-1 w-1 -translate-y-1/2 rounded-full bg-cyan-400 shadow-[0_0_10px_rgba(34,211,238,0.8)] animate-[flow3_8s_linear_infinite_4s]" />

  </div>

  <div className="grid gap-6 lg:grid-cols-4">
    {steps.map((step, index) => {
      const Icon = step.icon;

      return (
        <div
          key={step.title}
          className="group relative"
        >
          <div className="rounded-3xl border border-primary/10 bg-gradient-to-br from-card/80 via-card/60 to-primary/[0.02] p-6 backdrop-blur-xl transition-all duration-300 hover:-translate-y-1 hover:border-primary/30 hover:shadow-[0_0_30px_rgba(139,92,246,0.12)]">
            <div className="mb-5 flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-primary/20 to-purple-500/20 shadow-[0_0_20px_rgba(139,92,246,0.12)]">
              <Icon
                size={22}
                className="text-primary"
              />
            </div>

            <div className="mb-2 text-xs font-medium uppercase tracking-wider text-primary">
              Step {index + 1}
            </div>

            <h3 className="mb-2 text-lg font-semibold">
              {step.title}
            </h3>

            <p className="text-sm text-muted-foreground">
              {step.description}
            </p>
          </div>

          {index !== steps.length - 1 && (
            <div className="flex justify-center py-2 lg:hidden">
              <ArrowRight className="h-6 w-6 rotate-90 text-primary/50" />
            </div>
          )}
        </div>
      );
    })}
  </div>
</div>
      </div>
    </section>
  );
};

export default HowItWorks;