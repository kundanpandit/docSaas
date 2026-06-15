import { Button } from "@/components/ui/button";
import { Sparkles, FileText, ShieldCheck, Zap, FileStack, WandSparkles, ArrowRight, Upload,} from "lucide-react";
import { useEffect,useState } from "react";
import { AnimatePresence, motion } from "framer-motion";

const HeroSection = () => {
  const rotatingFeatures = [
    "Convert PDFs Instantly",
    "Compress Large Documents",
    "Extract Text with OCR",
    "Chat With PDFs Using AI",
    "Generate Smart Summaries",
  ];
  const [activeFeature, setActiveFeature] = useState(0);
  useEffect(() => {
    const interval = setInterval(() => {
      setActiveFeature((prev) => (prev + 1) % rotatingFeatures.length);
    }, 2000);
    return () => clearInterval(interval);
  }, []);

  const words=["PDF","Image","Word File","Scan Document","Invoice","Contract","Report"];

  const [text, setText] = useState("");
  const [wordIndex, setWordIndex] = useState(0);
  const [isDeleting, setIsDeleting] = useState(false);
  useEffect(() => {
    const currentWord = words[wordIndex];
    const timeout = setTimeout(
      () => {
        if (!isDeleting) {
          setText(currentWord.substring(0, text.length + 1));
          if (text === currentWord) {
            setTimeout(() => setIsDeleting(true), 1000);
          }
        } else {
          setText(currentWord.substring(0, text.length - 1));
          if (text === "") {
            setIsDeleting(false);
            setWordIndex((prev) => (prev + 1) % words.length);
          }
        }
      },
      isDeleting ? 50 : 100,
    );
    return () => clearTimeout(timeout);
  }, [text, isDeleting, wordIndex]);


  return (
    <section className="relative overflow-hidden pt-8 pb-12 md:pt-12 md:pb-16 lg:py-0">
      {/* Background Glow */}
      <div className="absolute inset-0 -z-10 overflow-hidden pointer-events-none">
        <div className="absolute left-[10%] top-[10%] h-137.5 w-137.5 rounded-full bg-primary/15 blur-[150px] animate-[orb1_25s_ease-in-out_infinite]" />
        <div className="absolute right-[10%] bottom-[10%] h-125 w-125 rounded-full bg-violet-500/10 blur-[150px] animate-[orb2_30s_ease-in-out_infinite]" />
        <div className="absolute left-1/2 top-1/2 h-100 w-100 -translate-x-1/2 -translate-y-1/2 rounded-full bg-cyan-500/10 blur-[130px] animate-[orb3_35s_ease-in-out_infinite]" />
      </div>

      <div className="container mx-auto px-5 md:px-6">
       <div className="grid items-center gap-10 lg:min-h-[85vh] lg:gap-12 lg:grid-cols-2">
          {/* LEFT SIDE */}
          <div>
            <div
              className="
                mb-6
                inline-flex
                items-center
                gap-2
                rounded-full
                border
                px-4
                py-2
                text-sm
              "
            >
              <Sparkles size={16} />
              AI-Powered Document Workspace
            </div>

            <h1 className="text-4xl font-bold leading-tight sm:text-5xl md:text-6xl lg:text-7xl">
              Upload Any
              <span className="block bg-linear-to-r from-primary via-purple-500 to-cyan-500 bg-clip-text text-transparent">
                {text}<span className="animate-pulse">|</span>
              </span>
            </h1>

            <p className="mt-5 max-w-full text-base text-muted-foreground sm:text-lg lg:max-w-xl">
              Convert, Compress, OCR, Summarize and Chat with your documents
              using powerful AI.
            </p>

            <div className="mt-4 flex items-center gap-2 overflow-hidden text-base font-semibold text-primary sm:text-lg">
            <Sparkles className="h-4 w-4 shrink-0" />
            <AnimatePresence mode="wait">
              <motion.span key={activeFeature} initial={{opacity:0,y:10}} animate={{opacity:1,y:0}} exit={{opacity:0,y:-10}} transition={{duration:.35,ease:"easeOut"}}>
                {rotatingFeatures[activeFeature]}
              </motion.span>
            </AnimatePresence>
          </div>

            {/* FEATURES */}
           <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:gap-5">
              <div className="flex items-center gap-2">
                <Zap size={18} />
                <span>20+ Tools</span>
              </div>

              <div className="flex items-center gap-2">
                <ShieldCheck size={18} />
                <span>Secure Processing</span>
              </div>

              <div className="flex items-center gap-2">
                <FileText size={18} />
                <span>Guest Access</span>
              </div>
            </div>

            {/* BUTTONS */}
            <div className="mt-8 flex flex-wrap gap-4 ">
              <Button size="lg">
               Get Started
                <ArrowRight className="ml-2" />
              </Button>

              <Button variant="outline" size="lg" >
                Explore Tools
              </Button>
            </div>
          </div>

          {/* RIGHT SIDE */}
          <div className="flex justify-center">
            <div className="w-full max-w-lg lg:order-last rounded-3xl border border-primary/10 bg-card/60 p-8 backdrop-blur-xl shadow-[0_0_40px_rgba(139,92,246,0.10)] transition-all duration-300 hover:-translate-y-1 hover:border-primary/40 hover:shadow-[0_0_60px_rgba(139,92,246,0.25)]">
              <div className="rounded-2xl border-2 border-dashed p-12 text-center transition-all duration-300 hover:border-primary/50">
                <Upload
                  size={48}
                  className="
                    mx-auto
                    mb-4
                    text-primary
                  "
                />

                <h3
                  className="
                    mb-2
                    text-xl
                    font-semibold
                  "
                >
                  Drop files here
                </h3>

                <p className="text-muted-foreground">
                  PDF, Word, JPG, PNG, WEBP
                </p>

                <Button className="mt-6">Select Files</Button>
              </div>

              {/* STATS */}
              <div className="mt-6 grid grid-cols-3 gap-4">
              <div className="flex items-center justify-center gap-3 rounded-xl">
                <FileStack className="h-6 w-6 text-primary" />
                <div>
                  <p className="font-semibold leading-none">20+</p>
                  <p className="text-xs text-muted-foreground">Tools</p>
                </div>
              </div>

              <div className="flex items-center justify-center gap-3 ">
              <WandSparkles className="h-6 w-6 text-primary" />
              <div>
                <p className="font-semibold leading-none">AI</p>
                <p className="text-xs text-muted-foreground">Powered</p>
              </div>
            </div>

              <div className="flex items-center justify-center gap-3 ">
              <ShieldCheck className="h-6 w-6 text-primary" />
              <div>
                <p className="font-semibold leading-none">100%</p>
                <p className="text-xs text-muted-foreground">Secure</p>
              </div>
            </div>
            </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};

export default HeroSection;
