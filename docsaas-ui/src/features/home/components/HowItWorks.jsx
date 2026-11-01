import { motion } from "framer-motion";
import { Download, SlidersHorizontal, Upload } from "lucide-react";

import Container from "@/components/common/Container";
import SectionHeading from "@/components/common/SectionHeading";

const STEPS = [
  { icon: Upload, title: "Add your files", text: "Drop PDFs or images onto the page, or pick them from your device." },
  { icon: SlidersHorizontal, title: "Choose options", text: "Set page ranges, quality or layout. Sensible defaults are already picked." },
  { icon: Download, title: "Download", text: "Your result is ready in seconds. Files are deleted automatically afterwards." },
];

const HowItWorks = () => (
  <section id="how-it-works" className="py-16 md:py-24">
    <Container>
      <SectionHeading eyebrow="How it works" title="Three steps. No learning curve." />

      <ol className="relative mt-12 grid gap-4 md:grid-cols-3">
        {/* connecting line behind the step numbers */}
        <div aria-hidden="true" className="absolute left-[16%] right-[16%] top-11 hidden h-px bg-gradient-to-r from-transparent via-primary/40 to-transparent md:block" />

        {STEPS.map((step, i) => (
          <motion.li
            key={step.title}
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-60px" }}
            transition={{ duration: 0.5, delay: i * 0.1 }}
            className="glass relative rounded-2xl p-6 text-center"
          >
            <span className="bg-brand-gradient relative mx-auto flex h-11 w-11 items-center justify-center rounded-xl text-white shadow-[0_8px_20px_-8px_var(--primary)]">
              <step.icon size={20} />
              <span className="absolute -right-2 -top-2 flex h-5 w-5 items-center justify-center rounded-full bg-background text-[11px] font-semibold text-foreground ring-1 ring-border">
                {i + 1}
              </span>
            </span>
            <h3 className="mt-5 font-semibold">{step.title}</h3>
            <p className="mt-1.5 text-sm text-muted-foreground">{step.text}</p>
          </motion.li>
        ))}
      </ol>
    </Container>
  </section>
);

export default HowItWorks;
