import usePageTitle from "@/hooks/usePageTitle";
import Hero from "../components/Hero";
import ToolExplorer from "../components/ToolExplorer";
import HowItWorks from "../components/HowItWorks";
import WhyDocSaaS from "../components/WhyDocSaaS";
import FaqSection from "../components/FaqSection";
import CtaSection from "../components/CtaSection";

const HomePage = () => {
  usePageTitle(null, "Free online PDF and image tools: merge, split, compress and convert. No sign-up, files auto-deleted.");

  return (
    <>
      <Hero />
      <ToolExplorer />
      <HowItWorks />
      <WhyDocSaaS />
      <FaqSection />
      <CtaSection />
    </>
  );
};

export default HomePage;
