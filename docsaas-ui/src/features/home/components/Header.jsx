import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { useSelector } from "react-redux";
import { AnimatePresence, motion } from "framer-motion";
import { ChevronDown, Lock, Menu, X } from "lucide-react";

import { Button } from "@/components/ui/button";
import Container from "@/components/common/Container";
import Logo from "@/components/common/Logo";
import UserMenu from "@/components/layout/UserMenu";
import HeaderThemeToggle from "@/features/theme/components/HeaderThemeToggle";
import UsageWidget from "@/features/usage/components/UsageWidget";
import { selectIsAuthenticated } from "@/features/auth/selectors/authSelectors";
import { TOOL_CATEGORIES } from "@/features/home/constants/tools";
import { cn } from "@/lib/utils";

const NAV_LINKS = [
  { label: "How it works", href: "/#how-it-works" },
  { label: "FAQ", href: "/#faq" },
];

/** Mega menu listing every tool by category. Opens on hover or keyboard focus. */
const ToolsMenu = () => {
  const { pathname } = useLocation();
  const locked = !useSelector(selectIsAuthenticated);
  // Remember which page the menu was opened on: navigating anywhere closes it.
  const [openAt, setOpenAt] = useState(null);
  const open = openAt === pathname;
  const setOpen = (value) => setOpenAt(value ? pathname : null);

  return (
    <div
      className="relative"
      onMouseEnter={() => setOpen(true)}
      onMouseLeave={() => setOpen(false)}
      onKeyDown={(e) => e.key === "Escape" && setOpen(false)}
    >
      <button
        type="button"
        aria-expanded={open}
        aria-haspopup="true"
        onClick={() => setOpen(!open)}
        className="flex items-center gap-1 rounded-full px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
      >
        Tools
        <ChevronDown size={15} className={cn("transition-transform duration-200", open && "rotate-180")} />
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, y: 8, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 8, scale: 0.98 }}
            transition={{ duration: 0.16 }}
            className="absolute left-1/2 top-full z-50 w-[640px] -translate-x-1/2 pt-3"
          >
            <div className="surface-solid grid grid-cols-4 gap-1 rounded-2xl p-3">
              {TOOL_CATEGORIES.map((category) => (
                <div key={category.id}>
                  <p className="px-2.5 pb-1.5 pt-1 text-xs font-medium uppercase tracking-wide text-muted-foreground">
                    {category.label}
                  </p>
                  {category.tools.map((tool) => (
                    <Link
                      key={tool.id}
                      to={`/tools/${tool.id}`}
                      className="flex items-center justify-between gap-2 rounded-lg px-2.5 py-1.5 text-sm transition-colors hover:bg-accent focus-visible:bg-accent focus-visible:outline-none"
                    >
                      <span className={cn(!tool.available && "text-muted-foreground")}>{tool.title}</span>
                      {!tool.available && <span className="text-[10px] text-muted-foreground">Soon</span>}
                      {tool.available && locked && category.id === "ai" && (
                        <Lock size={12} className="shrink-0 text-muted-foreground" aria-label="Log in to use" />
                      )}
                    </Link>
                  ))}
                </div>
              ))}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

const MobileMenu = ({ open, onClose, isAuthenticated }) => (
  <AnimatePresence>
    {open && (
      <>
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          onClick={onClose}
          className="fixed inset-0 z-40 bg-background/40 backdrop-blur-sm lg:hidden"
        />
        <motion.nav
          initial={{ opacity: 0, y: -12, scale: 0.98 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          exit={{ opacity: 0, y: -12, scale: 0.98 }}
          transition={{ duration: 0.18 }}
          aria-label="Mobile"
          className="surface-solid fixed inset-x-4 top-20 z-50 max-h-[calc(100dvh-6rem)] overflow-y-auto rounded-2xl p-4 lg:hidden"
        >
          {TOOL_CATEGORIES.map((category) => (
            <div key={category.id} className="mb-3">
              <p className="px-2 pb-1 text-xs font-medium uppercase tracking-wide text-muted-foreground">
                {category.label}
              </p>
              <div className="grid grid-cols-2 gap-1">
                {category.tools.map((tool) => (
                  <Link
                    key={tool.id}
                    to={`/tools/${tool.id}`}
                    onClick={onClose}
                    className={cn("rounded-lg px-2 py-2 text-sm hover:bg-accent", !tool.available && "text-muted-foreground")}
                  >
                    {tool.title}
                  </Link>
                ))}
              </div>
            </div>
          ))}

          <div className="mt-2 flex flex-col gap-1 border-t pt-3">
            {NAV_LINKS.map((link) => (
              <a key={link.href} href={link.href} onClick={onClose} className="rounded-lg px-2 py-2 text-sm hover:bg-accent">
                {link.label}
              </a>
            ))}
          </div>

          <div className="mt-3 grid grid-cols-2 gap-2">
            {isAuthenticated ? (
              <>
                <Button asChild variant="outline" size="lg" className="h-11 rounded-xl">
                  <Link to="/jobs" onClick={onClose}>My jobs</Link>
                </Button>
                <Button asChild variant="outline" size="lg" className="h-11 rounded-xl">
                  <Link to="/credits" onClick={onClose}>Credits</Link>
                </Button>
              </>
            ) : (
              <>
                <Button asChild variant="outline" size="lg" className="h-11 rounded-xl">
                  <Link to="/login" onClick={onClose}>Log in</Link>
                </Button>
                <Button asChild size="lg" className="h-11 rounded-xl">
                  <Link to="/register" onClick={onClose}>Sign up free</Link>
                </Button>
              </>
            )}
          </div>
        </motion.nav>
      </>
    )}
  </AnimatePresence>
);

const Header = () => {
  const [mobileOpenAt, setMobileOpenAt] = useState(null);
  const [scrolled, setScrolled] = useState(false);
  const isAuthenticated = useSelector(selectIsAuthenticated);
  const { pathname } = useLocation();

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  const mobileOpen = mobileOpenAt === pathname;
  const setMobileOpen = (value) => setMobileOpenAt(value ? pathname : null);

  return (
    <header className="fixed inset-x-0 top-0 z-50 pt-3">
      <Container>
        <div className="relative z-50 flex h-14 items-center justify-between rounded-2xl pl-3 pr-2">
          {/* The glass sits on its own layer behind the content. If the bar itself had backdrop-filter,
              Chrome would composite the Tools dropdown (a child that overflows the bar) see-through. */}
          <div
            aria-hidden="true"
            className={cn(
              "pointer-events-none absolute inset-0 -z-10 rounded-2xl transition-all duration-300",
              scrolled || mobileOpen ? "surface-solid" : "border border-transparent"
            )}
          />
          <Logo />

          <nav aria-label="Main" className="hidden items-center gap-1 lg:flex">
            <ToolsMenu />
            {NAV_LINKS.map((link) => (
              <a
                key={link.href}
                href={link.href}
                className="rounded-full px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
              >
                {link.label}
              </a>
            ))}
          </nav>

          <div className="flex items-center gap-1.5">
            <UsageWidget />
            <HeaderThemeToggle />

            {isAuthenticated ? (
              <UserMenu />
            ) : (
              <>
                <Button asChild variant="ghost" size="lg" className="hidden h-9 rounded-xl px-3 sm:flex">
                  <Link to="/login">Log in</Link>
                </Button>
                <Button asChild size="lg" className="hidden h-9 rounded-xl px-4 shadow-[0_6px_20px_-8px_var(--primary)] sm:flex">
                  <Link to="/register">Sign up free</Link>
                </Button>
              </>
            )}

            <Button
              size="icon-lg"
              variant="ghost"
              className="rounded-xl lg:hidden"
              aria-label={mobileOpen ? "Close menu" : "Open menu"}
              aria-expanded={mobileOpen}
              onClick={() => setMobileOpen(!mobileOpen)}
            >
              {mobileOpen ? <X /> : <Menu />}
            </Button>
          </div>
        </div>
      </Container>

      <MobileMenu open={mobileOpen} onClose={() => setMobileOpen(false)} isAuthenticated={isAuthenticated} />
    </header>
  );
};

export default Header;
