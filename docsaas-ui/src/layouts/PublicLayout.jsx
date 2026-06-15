import { Outlet } from "react-router-dom";

import PublicHeader from "@/features/home/components/PublicHeader";
import PublicFooter from "@/features/home/components/PublicFooter";

const PublicLayout = () => {
  return (
    <div className="min-h-screen flex flex-col">
      <PublicHeader />

      <main className="flex-1">
        <Outlet />
      </main>

      <PublicFooter />
    </div>
  );
};

export default PublicLayout;