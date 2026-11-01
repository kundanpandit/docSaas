import { Link } from "react-router-dom";

import Container from "@/components/common/Container";
import SectionHeading from "@/components/common/SectionHeading";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
import { MAX_FILE_SIZE_MB, RETENTION_MINUTES } from "@/features/home/constants/tools";

// Keep answers true to what is live today; update as features ship.
const faqs = [
  {
    question: "Do I need an account?",
    answer: "No. All live tools work without signing up. An account adds a job history and a higher hourly limit.",
  },
  {
    question: "Is DocSaaS free?",
    answer: "Yes, the tools are free to use. Guests have a fair-use limit per hour to keep the service fast for everyone.",
  },
  {
    question: "What happens to my files?",
    answer: `Your uploads are deleted right after processing. The result is kept for ${RETENTION_MINUTES} minutes so you can download it, then deleted automatically.`,
  },
  {
    question: "Which file types are supported?",
    answer: `PDF, Word (DOCX and DOC), JPG, PNG and WEBP, up to ${MAX_FILE_SIZE_MB} MB per file. Scanned documents can be read with OCR in English and Hindi.`,
  },
  {
    question: "Can I use password-protected PDFs?",
    answer: "Not yet. Please remove the password first; unlocking support is planned.",
  },
  {
    question: "How accurate are the AI summaries and chat answers?",
    answer: (
      <>
        The AI only uses the text of your document and cites page numbers, and it tells you when something is not in the
        document. It can still make mistakes, so check important details against the original.{" "}
        <Link to="/tools/chat-with-pdf" className="font-medium text-primary hover:underline">
          Try Chat With PDF
        </Link>
        .
      </>
    ),
  },
];

const FaqSection = () => (
  <section id="faq" className="py-16 md:py-24">
    <Container className="max-w-3xl">
      <SectionHeading eyebrow="FAQ" title="Questions, answered" />

      <Accordion type="single" collapsible className="glass mt-10 rounded-2xl px-2 py-1">
        {faqs.map((faq) => (
          <AccordionItem key={faq.question} value={faq.question} className="px-4">
            <AccordionTrigger className="py-4 text-base hover:no-underline">{faq.question}</AccordionTrigger>
            <AccordionContent className="pb-4 text-muted-foreground">{faq.answer}</AccordionContent>
          </AccordionItem>
        ))}
      </Accordion>
    </Container>
  </section>
);

export default FaqSection;
