import { Outlet } from "react-router-dom";

import Header from "@/features/home/components/Header";
import Footer from "@/features/home/components/Footer";
import AuroraBackground from "@/components/effects/AuroraBackground";

/** Public site: floating glass header, aurora background, glass footer. Pages add their own top spacing. */
const PublicLayout = () => {
  return (
    <div className="relative flex min-h-screen flex-col">
      <AuroraBackground />
      <Header />

      <main className="flex-1">
        <Outlet />
      </main>

      <Footer />
    </div>
  );
};

export default PublicLayout;
