import { Link, useRouteError } from "react-router-dom";
import { AlertTriangle } from "lucide-react";

import { Button } from "@/components/ui/button";
import AuroraBackground from "@/components/effects/AuroraBackground";

const ErrorPage = () => {
  const error = useRouteError();

  return (
    <div className="relative flex min-h-screen items-center justify-center px-4">
      <AuroraBackground />
      <div className="glass w-full max-w-md rounded-3xl px-6 py-12 text-center">
        <span className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-destructive/12 text-destructive">
          <AlertTriangle size={26} />
        </span>
        <h1 className="mt-5 text-2xl font-semibold">Something went wrong</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          {error?.statusText || error?.message || "An unexpected error occurred."}
        </p>
        <div className="mt-8 flex justify-center gap-2">
          <Button onClick={() => window.location.reload()} className="h-11 rounded-xl px-5">Reload</Button>
          <Button asChild variant="outline" className="h-11 rounded-xl px-5">
            <Link to="/">Home</Link>
          </Button>
        </div>
      </div>
    </div>
  );
};

export default ErrorPage;
