import Header from "../components/Header";
import HeroSection from "../components/HeroSection";
import ToolGrid from "../components/ToolGrid";
import MessageSlider from "../components/MessageSlider";
import SectionDivider from "../components/SectionDivider";

import ToolCategories from "../components/ToolCategories";

import { PDF_TOOLS, OCR_TOOLS, IMAGE_TOOLS, AI_TOOLS,} from "../constants/tools";
import HowItWorks from "../components/HowItWorks";
import FaqSection from "../components/FaqSection";
import CtaSection from "../components/CtaSection";
import Footer from "../components/Footer";


const HomePage1 = () => {
  return (
    <>
      <Header />

    <main className="pt-18">
  <HeroSection />

  <MessageSlider />

  <ToolCategories />

<SectionDivider />

  <div className="container mx-auto px-5 md:px-6 py-16">
    

    <div className="space-y-24">
      <section id="tools">
        <ToolGrid
          title="PDF Tools"
          tools={PDF_TOOLS}
        />
      </section>

      <ToolGrid
        title="OCR Tools"
        tools={OCR_TOOLS}
      />

      <ToolGrid
        title="Image Tools"
        tools={IMAGE_TOOLS}
      />

      <section id="ai-features">
        <ToolGrid
          title="AI Tools"
          tools={AI_TOOLS}
        />
      </section>
      <SectionDivider />
    </div>
  </div>
  <HowItWorks />

    <SectionDivider />

    <FaqSection/>

    <SectionDivider />

    <CtaSection />

    <Footer />
</main>
      
    </>
  );
};

export default HomePage1;