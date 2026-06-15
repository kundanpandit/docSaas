import { Sparkles, Brain, FileText, ScanText, RefreshCw, ImageIcon } from "lucide-react";

const categories = [
  { title: "AI Tools", description: "Summarize PDFs, chat with documents and extract insights using AI.", tools: ["AI Summary", "Chat With PDF", "Document Insights"], badge: "3+ AI Tools", icon: Brain },
  { title: "PDF Tools", description: "Merge, split, compress and organize PDFs effortlessly.", tools: ["Merge PDF", "Split PDF", "Compress PDF", "Extract Pages"], badge: "8+ Tools", icon: FileText },
  { title: "OCR", description: "Convert scanned PDFs and images into searchable text.", tools: ["Image To Text", "Scanned PDF OCR"], badge: "OCR Ready", icon: ScanText },
  { title: "Conversions", description: "Convert between PDF, Word and Images instantly.", tools: ["PDF ↔ Word", "PDF ↔ Images"], badge: "Fast Convert", icon: RefreshCw },
  { title: "Image Tools", description: "Compress, convert and optimize images.", tools: ["Compress", "Convert", "Resize"], badge: "Image Ready", icon: ImageIcon },
];

const ToolCategories = () => {
  const largeCards = categories.slice(0, 2);
  const smallCards = categories.slice(2);

  return (
    <section className="relative overflow-hidden pt-20 pb-10  md:pt-28 md:pb-12">
       <div className="absolute inset-0 -z-10 overflow-hidden pointer-events-none">
        <div className="absolute left-[15%] top-[10%] h-[400px] w-[400px] rounded-full bg-gradient-to-br from-primary/20 to-purple-500/20   shadow-[0_0_20px_rgba(139,92,246,0.15)]   blur-[140px] animate-[orb1_40s_linear_infinite]" />
        <div className="absolute right-[15%] bottom-[10%] h-[350px] w-[350px] rounded-full bg-violet-500/10 blur-[140px] animate-[orb2_50s_linear_infinite]" />
        </div>

      <div className="container pb-16 mx-auto px-5 md:px-6">
        <div className="mx-auto mb-14 max-w-3xl text-center">
          <div className="mb-4 inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/5 px-3 py-1 text-sm transition-all duration-300 hover:border-primary/40 hover:bg-primary/10 border-primary/30 bg-gradient-to-br from-primary/20 to-purple-500/20   shadow-[0_0_20px_rgba(139,92,246,0.15)]   shadow-[0_0_20px_rgba(139,92,246,0.15)] px-4 py-2 text-sm text-primary">
            <Sparkles className="h-4 w-4" />
            Everything You Need
          </div>

         <h2 className="text-4xl font-bold tracking-tight md:text-5xl lg:text-6xl">
            Powerful
            <span className="block bg-linear-to-r from-primary via-purple-400 to-cyan-400 bg-clip-text text-transparent pt-1">
                PDF, Image & AI Tools
            </span>
            </h2>

          <p className="mt-5 text-lg text-muted-foreground">
            Your complete AI-powered platform for PDFs, images, OCR, conversions and intelligent document processing.
          </p>
        </div>

        <div className="grid gap-6 md:grid-cols-2">
          {largeCards.map((item) => {
            const Icon = item.icon;

            return (
              <div key={item.title} className="group rounded-3xl border border-primary/10 bg-gradient-to-br from-card/80 via-card/60 to-primary/[0.03] hover:shadow-[0_0_50px_rgba(139,92,246,0.18)] p-8 backdrop-blur-xl transition-all duration-300 hover:-translate-y-2 hover:border-primary/30 hover:shadow-[0_0_40px_rgba(139,92,246,0.15)]">
                <div className="mb-6 flex items-center justify-between">
                  <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-primary/20 to-purple-500/20   shadow-[0_0_20px_rgba(139,92,246,0.15)]  ">
                    <Icon className="h-6 w-6 text-primary" />
                  </div>

                  <span className="rounded-full border border-primary/20 bg-primary/5 px-3 py-1 text-sm transition-all duration-300 hover:border-primary/40 hover:bg-primary/10 border-primary/20 bg-primary/5 px-3 py-1 text-xs text-primary">
                    {item.badge}
                  </span>
                </div>

                <h3 className="text-2xl font-bold">{item.title}</h3>

                <p className="mt-3 text-muted-foreground">
                  {item.description}
                </p>

                <div className="mt-6 flex flex-wrap gap-2">
                  {item.tools.map((tool) => (
                    <span key={tool} className="rounded-full border border-primary/20 bg-primary/5 px-3 py-1 text-sm transition-all duration-300 hover:border-primary/40 hover:bg-primary/10 px-3 py-1 text-sm">
                      {tool}
                    </span>
                  ))}
                </div>
              </div>
            );
          })}

          <div className="grid gap-6 md:col-span-2 md:grid-cols-3">
            {smallCards.map((item) => {
              const Icon = item.icon;

              return (
                <div key={item.title} className="group rounded-3xl border border-primary/10 bg-gradient-to-br from-card/80 via-card/60 to-primary/[0.03]  hover:shadow-[0_0_50px_rgba(139,92,246,0.18)] p-6 backdrop-blur-xl transition-all duration-300 hover:-translate-y-2 hover:border-primary/30 hover:shadow-[0_0_30px_rgba(139,92,246,0.12)]">
                  <div className="mb-4 flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-primary/20 to-purple-500/20   shadow-[0_0_20px_rgba(139,92,246,0.15)]  ">
                    <Icon className="h-5 w-5 text-primary" />
                  </div>

                  <h3 className="text-xl font-semibold">{item.title}</h3>

                  <p className="mt-2 text-sm text-muted-foreground">
                    {item.description}
                  </p>

                  <div className="mt-4 flex flex-wrap gap-2">
                    {item.tools.map((tool) => (
                      <span key={tool} className="rounded-full border border-primary/20 bg-primary/5 px-3 py-1 text-sm transition-all duration-300 hover:border-primary/40 hover:bg-primary/10 px-2 py-1 text-xs">
                        {tool}
                      </span>
                    ))}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </section>
  );
};

export default ToolCategories;