import { Outlet } from "react-router-dom";

import AppShell from "@/components/layout/AppShell";
import Sidebar from "@/components/layout/Sidebar";
import Header from "@/components/layout/Header";

const MainLayout = () => {
  return (
    <AppShell>
      <div className="flex min-h-screen">
      <Sidebar />

      <div className="flex flex-1 flex-col">
        <Header />

        <main className="flex-1 p-6">
          <Outlet />
        </main>
      </div>
    </div>
    </AppShell>
  );
};

export default MainLayout;