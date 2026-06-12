import { Outlet } from "react-router-dom";

const AuthLayout = () => {
  return (
    <div className="relative min-h-screen overflow-hidden bg-background">

      {/* Blue Glow */}
      <div
        className="
          absolute
          left-1/2
          top-1/3
          h-96
          w-96
          -translate-x-1/2
          rounded-full
         bg-blue-500/5 dark:bg-blue-500/15
          blur-3xl
        "
      />

      {/* Content */}
      <div className="relative z-10 container mx-auto flex min-h-screen items-center justify-center px-4 py-8 sm:py-12">
        <div className="w-full max-w-md rounded-3xl border border-border/50 bg-card p-6 shadow-2xl sm:p-8 md:p-10">
          <Outlet />
        </div>
      </div>

    </div>
  );
};

export default AuthLayout;