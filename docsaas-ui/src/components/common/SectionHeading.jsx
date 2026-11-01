import { cn } from "@/lib/utils";

const SectionHeading = ({ eyebrow, title, description, className, align = "center" }) => (
  <div className={cn("max-w-2xl", align === "center" && "mx-auto text-center", className)}>
    {eyebrow && (
      <p className="mb-3 text-sm font-medium text-primary">{eyebrow}</p>
    )}
    <h2 className="text-3xl font-semibold tracking-tight text-balance md:text-4xl">{title}</h2>
    {description && (
      <p className="mt-3 text-base text-pretty text-muted-foreground md:text-lg">{description}</p>
    )}
  </div>
);

export default SectionHeading;
