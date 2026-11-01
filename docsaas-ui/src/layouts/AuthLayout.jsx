import { Outlet } from "react-router-dom";
import { motion } from "framer-motion";
import { Check } from "lucide-react";

import AuroraBackground from "@/components/effects/AuroraBackground";
import Logo from "@/components/common/Logo";
import HeaderThemeToggle from "@/features/theme/components/HeaderThemeToggle";
import DocStack3D from "@/features/home/components/DocStack3D";

const PERKS = ["30 credits a day, 10x the guest limit", "AI summary, insights and chat with PDF", "Job history for every file you process"];

const AuthLayout = () => {
  return (
    <div className="relative min-h-screen">
      <AuroraBackground />

      <div className="mx-auto grid min-h-screen w-full max-w-6xl items-center gap-10 px-4 py-6 sm:px-6 lg:grid-cols-2 lg:px-8">
        {/* brand panel, desktop only */}
        <div className="hidden lg:block">
          <Logo />
          <h2 className="mt-10 max-w-md text-4xl font-semibold tracking-tight text-balance">
            Your documents, <span className="text-gradient">sorted in seconds.</span>
          </h2>
          <ul className="mt-6 space-y-3 text-muted-foreground">
            {PERKS.map((perk) => (
              <li key={perk} className="flex items-center gap-2.5">
                <span className="flex h-5 w-5 items-center justify-center rounded-full bg-primary/15 text-primary">
                  <Check size={12} strokeWidth={3} />
                </span>
                {perk}
              </li>
            ))}
          </ul>
          <DocStack3D className="mt-4 h-[360px] scale-90" />
        </div>

        <div className="flex flex-col items-center">
          <div className="mb-6 flex w-full max-w-md items-center justify-between lg:justify-end">
            <Logo className="lg:hidden" />
            <HeaderThemeToggle />
          </div>

          <motion.div
            initial={{ opacity: 0, y: 16, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            transition={{ duration: 0.45, ease: [0.22, 1, 0.36, 1] }}
            className="glass w-full max-w-md rounded-3xl p-6 sm:p-8"
          >
            <Outlet />
          </motion.div>
        </div>
      </div>
    </div>
  );
};

export default AuthLayout;
