import ToolCard from "./ToolCard";

const descriptions = {
  "PDF Tools": "Merge, split, compress and convert PDF documents effortlessly.",
  "OCR Tools": "Extract text from images and scanned PDFs with intelligent OCR.",
  "Image Tools": "Convert, compress and optimize images in seconds.",
  "AI Tools": "Summarize documents and chat with PDFs using AI.",
};

const ToolGrid = ({ title, tools }) => {
  return (
    <section className="relative space-y-6 py-4">
      <div className="absolute inset-0 -z-10 overflow-hidden pointer-events-none">
        <div className="absolute left-[10%] top-[20%] h-[300px] w-[300px] rounded-full bg-primary/5 blur-[120px]" />
        <div className="absolute right-[10%] bottom-[10%] h-[250px] w-[250px] rounded-full bg-violet-500/5 blur-[120px]" />
      </div>

      <div>
        <h2 className="text-4xl font-bold tracking-tight md:text-5xl">
        {title.replace(" Tools", "")}
        <span className="bg-linear-to-r from-primary via-purple-400 to-cyan-400 bg-clip-text text-transparent">
          {" "}Tools
        </span>
      </h2>

        <p className="mt-3 max-w-2xl text-lg text-muted-foreground">
        {descriptions[title]}
      </p>
      </div>

      <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {tools.map((tool) => (
          <ToolCard
            key={tool.id}
            title={tool.title}
            description={tool.description}
            icon={tool.icon}
          />
        ))}
      </div>
    </section>
  );
};

export default ToolGrid;
