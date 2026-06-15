import { Link } from "react-router-dom";
import { ArrowRight, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";

const CtaSection = () => {
  return (
    <section className="relative overflow-hidden py-20 md:py-28">
      <div className="absolute inset-0 -z-10 overflow-hidden pointer-events-none">
        <div className="absolute left-[10%] top-[20%] h-[400px] w-[400px] rounded-full bg-primary/6 blur-[160px]" />
        <div className="absolute right-[10%] bottom-[10%] h-[400px] w-[400px] rounded-full bg-violet-500/6 blur-[160px]" />
      </div>

      <div className="container mx-auto px-5 md:px-6">
        <div className="relative mx-auto max-w-6xl overflow-hidden rounded-[40px] border border-primary/15 bg-gradient-to-br from-card/90 via-card/70 to-primary/[0.04] p-8 backdrop-blur-xl shadow-[0_0_80px_rgba(139,92,246,0.08)] transition-all duration-500 hover:border-primary/30 hover:shadow-[0_0_120px_rgba(139,92,246,0.18)] md:p-14">
          
          <div className="absolute inset-0 -z-10 overflow-hidden rounded-[40px]">
            <div className="absolute left-[10%] top-[20%] h-[300px] w-[300px] rounded-full bg-primary/15 blur-[120px]" />
            <div className="absolute right-[10%] bottom-[10%] h-[300px] w-[300px] rounded-full bg-violet-500/15 blur-[120px]" />
            <div className="absolute left-1/2 top-1/2 h-[250px] w-[250px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-cyan-500/10 blur-[100px]" />
          </div>

          <div className="relative text-center">
            <div className="mb-5 inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/5 px-4 py-2 text-sm text-primary">
              <Sparkles className="h-4 w-4" />
              Ready To Get Started?
            </div>

            <h2 className="text-4xl font-bold tracking-tight md:text-6xl lg:text-7xl">
              Transform Documents
              <span className="block bg-linear-to-r from-primary via-purple-400 to-cyan-400 bg-clip-text text-transparent">
                In Seconds
              </span>
            </h2>

            <p className="mx-auto mt-6 max-w-2xl text-sm leading-relaxed text-muted-foreground md:text-base">
                Convert, compress, OCR, summarize and chat with documents from one intelligent workspace powered by AI.
                </p>

            

            <div className="mt-10 flex flex-col justify-center gap-4 sm:flex-row">
              <Button
                asChild
                size="lg"
                className="min-w-[240px]"
              >
                <Link to="/register">
                  Create Free Account
                  <ArrowRight className="ml-2 h-4 w-4" />
                </Link>
              </Button>

              <Button
                asChild
                variant="outline"
                size="lg"
                className="min-w-[240px]"
              >
                <Link to="/login">
                  Login
                </Link>
              </Button>
            </div>

            <p className="mt-6 text-sm text-muted-foreground">
              No credit card required  •  Free guest access  •  Google Login bonus credits  •  Secure document processing
            </p>
          </div>
        </div>
      </div>
    </section>
  );
};

export default CtaSection;