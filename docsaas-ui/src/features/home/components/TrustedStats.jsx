import { ShieldCheck, Sparkles, ScanText, FileStack, User } from "lucide-react";


const TrustedStats = () => {
  const stats = [
    { icon: FileStack, value: "20+", label: "Tools" },
    { icon: Sparkles, value: "AI", label: "Powered" },
    { icon: ScanText, value: "OCR", label: "Ready" },
    { icon: ShieldCheck, value: "100%", label: "Secure" },
    { icon: User, value: "Guest", label: "Access" },
  ];
  const tools = [
    "Merge PDF",
    "Split PDF",
    "Compress PDF",
    "PDF To Word",
    "Word To PDF",
    "OCR",
    "Chat With PDF",
    "AI Summary",
    "Images To PDF",
    "Extract Pages",
  ];
  return (
    <section className="py-20">
      <div className="container mx-auto px-5 md:px-6">
        <div className="mx-auto max-w-3xl text-center">
          <h2 className="text-3xl font-bold md:text-4xl">
            Everything You Need To Work With Documents
          </h2>
          <p className="mt-4 text-muted-foreground">
            Powerful PDF, Image and AI tools in one workspace.
          </p>
        </div>
        <div className="mt-12 grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
          {stats.map((item, index) => {
            const Icon = item.icon;
            return (
              <div
                key={index}
                className="rounded-2xl border bg-card/50 p-6 text-center backdrop-blur-sm"
              >
                <Icon className="mx-auto mb-4 h-6 w-6 text-primary" />
                <h3 className="text-2xl font-bold">{item.value}</h3>
                <p className="mt-1 text-sm text-muted-foreground">
                  {item.label}
                </p>
              </div>
            );
          })}
        </div>
        <div className="mt-10 flex flex-wrap justify-center gap-3">
          {tools.map((tool, index) => (
            <div
              key={index}
              className="rounded-full border px-4 py-2 text-sm text-muted-foreground"
            >
              {tool}
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};
export default TrustedStats;
