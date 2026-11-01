import { motion } from "framer-motion";
import { Gauge, Gift, ShieldCheck, Smartphone } from "lucide-react";

import Container from "@/components/common/Container";
import SectionHeading from "@/components/common/SectionHeading";
import TiltCard from "@/components/effects/TiltCard";
import { RETENTION_MINUTES } from "@/features/home/constants/tools";
import { cn } from "@/lib/utils";

const FEATURES = [
  {
    icon: ShieldCheck,
    title: "Private by default",
    text: `Uploads are deleted as soon as processing finishes. Results are removed after ${RETENTION_MINUTES} minutes.`,
    wide: true,
  },
  { icon: Gift, title: "Free to use", text: "Core tools are free. No watermarks, no account needed." },
  { icon: Gauge, title: "Fast", text: "Most files are done in a few seconds." },
  {
    icon: Smartphone,
    title: "Works everywhere",
    text: "Phone, tablet or desktop: nothing to install, just open the page.",
    wide: true,
  },
];

const WhyDocSaaS = () => (
  <section className="py-16 md:py-24">
    <Container>
      <SectionHeading eyebrow="Why DocSaaS" title="Simple tools you can trust" />

      <div className="mt-12 grid gap-4 md:grid-cols-3">
        {FEATURES.map((f, i) => (
          <motion.div
            key={f.title}
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-60px" }}
            transition={{ duration: 0.5, delay: i * 0.08 }}
            className={cn(f.wide && "md:col-span-2")}
          >
            <TiltCard max={5} className="glass rounded-2xl p-6">
              <div style={{ transform: "translateZ(24px)" }}>
                <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/12 text-primary">
                  <f.icon size={20} />
                </span>
                <h3 className="mt-4 font-semibold">{f.title}</h3>
                <p className="mt-1.5 text-sm text-muted-foreground">{f.text}</p>
              </div>
            </TiltCard>
          </motion.div>
        ))}
      </div>
    </Container>
  </section>
);

export default WhyDocSaaS;
