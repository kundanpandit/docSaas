import { Link } from "react-router-dom";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";

const faqs = [
  {
    question: "How many free credits do I get as a guest user?",
    answer:
      "Guest users receive free credits to try DocSaaS tools without creating an account.",
  },
  {
    question: "What are the benefits of creating an account?",
    answer: (
      <>
        Registered users receive additional credits, access to job history,
        saved documents and future workspace features.{" "}
        <Link
          to="/register"
          className="font-medium text-primary hover:underline"
        >
          Create a free account
        </Link>
        .
      </>
    ),
  },
  {
    question: "Do I get bonus credits with Google Login?",
    answer: (
      <>
        Yes. Users who sign up using Google Login receive additional bonus
        credits.{" "}
        <Link
          to="/login"
          className="font-medium text-primary hover:underline"
        >
          Login now
        </Link>
        .
      </>
    ),
  },
  {
    question: "Can I upgrade my credits later?",
    answer:
      "Yes. Additional credit plans and premium features will be available in future releases.",
  },
  {
    question: "Do I need an account to use DocSaaS?",
    answer:
      "No. You can start using many tools as a guest and create an account later for additional credits and job history.",
  },
  {
    question: "Are my files secure?",
    answer:
      "Yes. Files are processed securely and automatically cleaned up after processing.",
  },
  {
    question: "Which file formats are supported?",
    answer:
      "DocSaaS supports PDF, JPG, PNG, WEBP, Word documents and other common document formats.",
  },
  {
    question: "Can I chat with my PDF using AI?",
    answer:
      "Yes. Upload a document and ask questions, generate summaries and extract key insights using AI.",
  },
  {
    question: "Do you support OCR?",
    answer:
      "Yes. You can extract searchable text from scanned PDFs and images using powerful OCR technology.",
  },
  {
    question: "Is DocSaaS free to use?",
    answer:
      "Yes. Free credits are available for guest and registered users. Additional credit options may be introduced in future plans.",
  },
];

const FaqSection = () => {
  return (
    <section className="relative overflow-hidden py-20 md:py-28">
      <div className="absolute inset-0 -z-10 overflow-hidden pointer-events-none">
        <div className="absolute left-[10%] top-[20%] h-[350px] w-[350px] rounded-full bg-primary/6 blur-[140px]" />
        <div className="absolute right-[10%] bottom-[10%] h-[350px] w-[350px] rounded-full bg-violet-500/6 blur-[140px]" />
      </div>

      <div className="container mx-auto px-5 md:px-6">
        <div className="mx-auto mb-14 max-w-3xl text-center">
          <div className="mb-4 inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/5 px-4 py-2 text-sm text-primary">
            Frequently Asked Questions
          </div>

          <h2 className="text-4xl font-bold tracking-tight md:text-5xl">
            Have
            <span className="bg-linear-to-r from-primary via-purple-400 to-cyan-400 bg-clip-text text-transparent">
              {" "}Questions?
            </span>
          </h2>

          <p className="mt-4 text-lg text-muted-foreground">
            Everything you need to know about DocSaaS, credits, security and document tools.
          </p>
        </div>

        <div className="mx-auto max-w-4xl">
          <Accordion
            type="single"
            collapsible
            className="space-y-4"
          >
            {faqs.map((faq, index) => (
              <AccordionItem
                key={index}
                value={`item-${index}`}
                className="overflow-hidden rounded-3xl border border-primary/10 bg-gradient-to-br from-card/80 via-card/60 to-primary/[0.02] px-6 backdrop-blur-xl transition-all duration-300 hover:border-primary/20 hover:shadow-[0_0_30px_rgba(139,92,246,0.08)]"
              >
                <AccordionTrigger className="py-6 text-left text-base font-semibold hover:no-underline">
                  {faq.question}
                </AccordionTrigger>

                <AccordionContent className="pb-6 text-muted-foreground">
                  {faq.answer}
                </AccordionContent>
              </AccordionItem>
            ))}
          </Accordion>
        </div>
      </div>
    </section>
  );
};

export default FaqSection;