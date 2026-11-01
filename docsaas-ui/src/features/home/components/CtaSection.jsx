import { Link } from "react-router-dom";
import { ArrowRight } from "lucide-react";

import { Button } from "@/components/ui/button";
import Container from "@/components/common/Container";

const CtaSection = () => (
  <section className="py-16 md:py-24">
    <Container>
      <div className="glass relative overflow-hidden rounded-3xl px-6 py-14 text-center md:py-16">
        <div aria-hidden="true" className="bg-brand-gradient absolute -top-24 left-1/2 h-48 w-[60%] -translate-x-1/2 rounded-full opacity-25 blur-3xl" />

        <h2 className="relative text-3xl font-semibold tracking-tight text-balance md:text-4xl">
          Ready when you are
        </h2>
        <p className="relative mx-auto mt-3 max-w-md text-muted-foreground">
          Pick a tool and finish your task in under a minute. No sign-up required.
        </p>

        <div className="relative mt-8 flex flex-wrap justify-center gap-3">
          <Button asChild size="lg" className="h-12 rounded-xl px-6 text-base shadow-[0_10px_30px_-10px_var(--primary)]">
            <a href="#tools">
              Browse tools
              <ArrowRight />
            </a>
          </Button>
          <Button asChild size="lg" variant="ghost" className="h-12 rounded-xl px-6 text-base">
            <Link to="/register">Create free account</Link>
          </Button>
        </div>
      </div>
    </Container>
  </section>
);

export default CtaSection;
