import { ArrowRight } from "lucide-react";

const ToolCard = ({
  title,
  description,
  icon: Icon,
}) => {
  return (
    <div className="group relative cursor-pointer overflow-hidden rounded-3xl border border-white/10 bg-gradient-to-br from-card/80 via-card/60 to-primary/[0.02] p-5 backdrop-blur-xl transition-all duration-300 hover:-translate-y-1 hover:border-primary/30 hover:shadow-[0_0_30px_rgba(139,92,246,0.12)]">
      <div className="absolute inset-0 bg-gradient-to-br from-primary/[0.03] via-purple-500/[0.02] to-cyan-500/[0.03] opacity-0 transition-opacity duration-300 group-hover:opacity-100" />

      <div className="relative z-10">
        <div className="mb-4 flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-br from-primary/20 to-purple-500/20 shadow-[0_0_20px_rgba(139,92,246,0.12)]">
          {Icon && (
            <Icon
              size={20}
              className="text-primary"
            />
          )}
        </div>

        <h3 className="mb-2 text-base font-semibold leading-tight">
          {title}
        </h3>

        <p className="line-clamp-2 text-sm text-muted-foreground">
          {description}
        </p>

        <div className="mt-4 flex items-center gap-2 text-sm font-medium text-primary">
          Try Now

          <ArrowRight
            size={15}
            className="transition-transform duration-300 group-hover:translate-x-1"
          />
        </div>
      </div>
    </div>
  );
};

export default ToolCard;