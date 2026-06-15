import { Sparkles } from "lucide-react";

const messages = [
  "🎁 Guest Users Get Free Credits To Try All Essential Tools",
"🔐 Create A Free Account To Unlock More Credits & Job History",
"🌐 Sign In With Google And Get Bonus Credits Instantly",
"🤖 AI PDF Summary & Chat With PDF Available",
"📄 Merge, Split, Compress, OCR & Convert Documents Instantly",
];

const MessageSlider = () => {
  return (
    <section className="border-y border-border bg-muted/20 overflow-hidden">
      <div className="relative flex overflow-hidden whitespace-nowrap py-3">
        <div className="animate-marquee flex min-w-full shrink-0 items-center gap-12 px-6">
          {messages.map((message, index) => (
            <div
              key={index}
              className="flex items-center gap-2 text-sm font-medium"
            >
              <Sparkles size={14} />
              {message}
            </div>
          ))}
        </div>

        <div className="animate-marquee flex min-w-full shrink-0 items-center gap-12 px-6">
          {messages.map((message, index) => (
            <div
              key={`duplicate-${index}`}
              className="flex items-center gap-2 text-sm font-medium"
            >
              <Sparkles size={14} />
              {message}
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default MessageSlider;