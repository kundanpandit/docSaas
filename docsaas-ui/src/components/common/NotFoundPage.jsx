import { Link } from "react-router-dom";
import { ArrowLeft } from "lucide-react";

import { Button } from "@/components/ui/button";
import Container from "@/components/common/Container";
import usePageTitle from "@/hooks/usePageTitle";

/** Rendered inside the public layout (header and footer come from there). */
const NotFoundPage = () => {
  usePageTitle("Page not found");

  return (
    <section className="pb-8 pt-32 md:pt-40">
      <Container className="max-w-xl">
        <div className="glass rounded-3xl px-6 py-14 text-center">
          <p className="text-gradient text-7xl font-semibold tracking-tight">404</p>
          <h1 className="mt-4 text-2xl font-semibold">Page not found</h1>
          <p className="mt-2 text-muted-foreground">The page you're looking for doesn't exist or has moved.</p>
          <Button asChild size="lg" className="mt-8 h-11 rounded-xl px-5">
            <Link to="/">
              <ArrowLeft />
              Back to home
            </Link>
          </Button>
        </div>
      </Container>
    </section>
  );
};

export default NotFoundPage;
