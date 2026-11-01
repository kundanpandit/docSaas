import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { ChevronRight } from "lucide-react";

import Container from "@/components/common/Container";

/** Shared frame for every tool page: breadcrumb, icon, title and description above the tool body. */
/** wide: full-width body for tools like the editor (header stays centered). */
const ToolLayout = ({ tool, children, aside, wide = false }) => {
  const Icon = tool.icon;

  return (
    <section className="pb-8 pt-28 md:pt-32">
      <Container className={wide ? "max-w-7xl" : "max-w-4xl"}>
        <nav aria-label="Breadcrumb" className="mb-8 flex items-center gap-1 text-sm text-muted-foreground">
          <Link to="/" className="hover:text-foreground">Home</Link>
          <ChevronRight size={14} />
          <Link to="/#tools" className="hover:text-foreground">Tools</Link>
          <ChevronRight size={14} />
          <span className="text-foreground">{tool.title}</span>
        </nav>

        <motion.header
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
          className="mb-10 text-center"
        >
          {Icon && (
            <span className="bg-brand-gradient mx-auto mb-5 flex h-14 w-14 items-center justify-center rounded-2xl text-white shadow-[0_12px_30px_-10px_var(--primary)]">
              <Icon size={26} />
            </span>
          )}
          <h1 className="text-3xl font-semibold tracking-tight md:text-4xl">{tool.title}</h1>
          <p className="mx-auto mt-3 max-w-xl text-pretty text-muted-foreground md:text-lg">{tool.description}</p>
        </motion.header>

        <motion.div
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.08, ease: [0.22, 1, 0.36, 1] }}
        >
          {children}
        </motion.div>

        {aside && <div className="mt-10">{aside}</div>}
      </Container>
    </section>
  );
};

export default ToolLayout;
