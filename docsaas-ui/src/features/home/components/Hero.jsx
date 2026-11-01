import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { ArrowRight, Lock, Sparkles, UserX, Zap } from "lucide-react";

import { Button } from "@/components/ui/button";
import Container from "@/components/common/Container";
import { ALL_TOOLS } from "@/features/home/constants/tools";
import DocStack3D from "./DocStack3D";

const POPULAR = ["merge-pdf", "compress-pdf", "split-pdf", "images-to-pdf"];

const rise = (delay) => ({
  initial: { opacity: 0, y: 18 },
  animate: { opacity: 1, y: 0 },
  transition: { duration: 0.6, delay, ease: [0.22, 1, 0.36, 1] },
});

const Hero = () => {
  const popular = POPULAR.map((id) => ALL_TOOLS.find((t) => t.id === id));
  const liveCount = ALL_TOOLS.filter((t) => t.available).length;

  return (
    <section className="pb-12 pt-32 md:pb-20 md:pt-40">
      <Container className="grid items-center gap-12 lg:grid-cols-[1.1fr_1fr]">
        <div className="text-center lg:text-left">
          <motion.div {...rise(0)} className="glass mb-6 inline-flex items-center gap-2 rounded-full px-3.5 py-1.5 text-xs font-medium">
            <Sparkles size={14} className="text-primary" />
            {liveCount} free tools · AI summary & chat included
          </motion.div>

          <motion.h1 {...rise(0.08)} className="text-4xl font-semibold tracking-tight text-balance sm:text-5xl lg:text-6xl">
            Every PDF tool you need, <span className="text-gradient">beautifully simple.</span>
          </motion.h1>

          <motion.p {...rise(0.16)} className="mx-auto mt-5 max-w-xl text-lg text-pretty text-muted-foreground lg:mx-0">
            Merge, split, compress and convert documents in seconds. Free, private, and no account required.
          </motion.p>

          <motion.div {...rise(0.24)} className="mt-8 flex flex-wrap justify-center gap-3 lg:justify-start">
            <Button asChild size="lg" className="h-12 rounded-xl px-6 text-base shadow-[0_10px_30px_-10px_var(--primary)] transition-transform hover:-translate-y-0.5">
              <a href="#tools">
                Explore tools
                <ArrowRight />
              </a>
            </Button>
            <Button asChild size="lg" variant="outline" className="glass h-12 rounded-xl px-6 text-base transition-transform hover:-translate-y-0.5">
              <Link to="/tools/compress-pdf">Compress a PDF</Link>
            </Button>
          </motion.div>

          <motion.div {...rise(0.32)} className="mt-8 flex flex-wrap items-center justify-center gap-2 lg:justify-start">
            <span className="text-sm text-muted-foreground">Popular:</span>
            {popular.map((tool) => (
              <Link
                key={tool.id}
                to={`/tools/${tool.id}`}
                className="rounded-full border bg-background/50 px-3 py-1 text-sm transition-all hover:-translate-y-0.5 hover:border-primary/40 hover:text-primary"
              >
                {tool.title}
              </Link>
            ))}
          </motion.div>

          <motion.ul {...rise(0.4)} className="mt-10 flex flex-wrap justify-center gap-x-6 gap-y-2 text-sm text-muted-foreground lg:justify-start">
            <li className="flex items-center gap-1.5"><UserX size={15} className="text-primary" /> No sign-up</li>
            <li className="flex items-center gap-1.5"><Lock size={15} className="text-primary" /> Auto-deleted files</li>
            <li className="flex items-center gap-1.5"><Zap size={15} className="text-primary" /> Done in seconds</li>
          </motion.ul>
        </div>

        <motion.div
          initial={{ opacity: 0, scale: 0.92 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.9, delay: 0.2, ease: [0.22, 1, 0.36, 1] }}
          className="hidden sm:block"
        >
          <DocStack3D />
        </motion.div>
      </Container>
    </section>
  );
};

export default Hero;
