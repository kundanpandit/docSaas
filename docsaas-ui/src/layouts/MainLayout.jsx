import { Outlet } from "react-router-dom";

import AppShell from "@/components/layout/AppShell";
import Sidebar from "@/components/layout/Sidebar";
import MobileSidebar from "@/components/layout/MobileSidebar";
import Header from "@/components/layout/Header";

const MainLayout = () => {
  return (
    <AppShell>
      <Sidebar />

      <div className="flex min-w-0 flex-1 flex-col gap-4">
        <Header />

        <main className="flex-1 pb-24 md:pb-4">
          <Outlet />
        </main>
      </div>

      <MobileSidebar />
    </AppShell>
  );
};

export default MainLayout;
