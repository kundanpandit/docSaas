import { Link } from "react-router-dom";
import {
  FileText,
  Sparkles,
  Menu,
  X,
  ChevronDown,
  SunMoon,
} from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

import { Button } from "@/components/ui/button";
import HeaderThemeToggle from "@/features/theme/components/HeaderThemeToggle";
import { useState } from "react";

const Header = () => {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  return (
    <>
      {/* Announcement Bar
    <div className="border-b border-border bg-background">
        <div className="container mx-auto flex h-9 items-center justify-center px-4 text-sm text-muted-foreground">
          🚀 Get 25 Free Credits on Google Sign Up
        </div>
      </div> */}

      {/* mobileMenuOpen */}

      <div
        onClick={() => setMobileMenuOpen(false)}
        className={`fixed inset-0 z-1000 bg-black/50 transition-opacity duration-300 ${mobileMenuOpen ? "opacity-100" : "pointer-events-none opacity-0"}`}
      />

      {/* Main Header */}

      <header className="fixed top-0 left-0 right-0 z-50 w-full border-b border-border/50 bg-background/75 backdrop-blur-xl transition-all duration-300 ">
        <div className="container mx-auto flex h-18 items-center justify-between px-4">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-primary to-purple-600 text-white shadow-lg">
              <FileText size={20} />
            </div>

            <div>
              <h2 className="text-xl font-bold">DocSaaS</h2>

              <p className="text-xs text-muted-foreground">
                AI Document Workspace
              </p>
            </div>
          </Link>

          {/* flex items-center gap-1 text-sm font-medium transition-colors hover:text-primary */}

          {/* Desktop Navigation */}
          <nav className="hidden lg:flex items-center gap-8">
            {/* Tools */}
            <DropdownMenu>
              <DropdownMenuTrigger className="flex items-center gap-1 text-sm font-medium transition-colors hover:text-primary outline-none">
                Tools
                <ChevronDown size={16} />
              </DropdownMenuTrigger>

              <DropdownMenuContent
                align="start"
                sideOffset={12}
                className="
                        w-56
                        rounded-2xl
                        border
                        bg-background/95
                        backdrop-blur-xl
                        p-2
                        shadow-2xl
                        data-[state=open]:animate-in
                        data-[state=closed]:animate-out
                        data-[state=closed]:fade-out-0
                        data-[state=open]:fade-in-0"
              >
                <DropdownMenuItem
                  className="
                        rounded-xl
                        cursor-pointer
                        py-3 "
                >
                  Merge PDF
                </DropdownMenuItem>

                <DropdownMenuItem
                  className="
                        rounded-xl
                        cursor-pointer
                        py-3 "
                >
                  Split PDF
                </DropdownMenuItem>

                <DropdownMenuItem
                  className="
                        rounded-xl
                        cursor-pointer
                        py-3 "
                >
                  Compress PDF
                </DropdownMenuItem>

                <DropdownMenuItem
                  className="
                        rounded-xl
                        cursor-pointer
                        py-3 "
                >
                  PDF to Word
                </DropdownMenuItem>

                <DropdownMenuItem
                  className="
                        rounded-xl
                        cursor-pointer
                        py-3 "
                >
                  Word to PDF
                </DropdownMenuItem>

                <DropdownMenuItem
                  className="
                        rounded-xl
                        cursor-pointer
                        py-3 "
                >
                  PDF to Images
                </DropdownMenuItem>

                <DropdownMenuItem
                  className="
                        rounded-xl
                        cursor-pointer
                        py-3 "
                >
                  Images to PDF
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>

            {/* AI Tools */}
            <DropdownMenu>
              <DropdownMenuTrigger className="flex items-center gap-1 text-sm font-medium transition-colors hover:text-primary outline-none">
                AI Tools
                <ChevronDown size={16} />
              </DropdownMenuTrigger>

              <DropdownMenuContent
                align="start"
                sideOffset={12}
                className="
                        w-56
                        rounded-2xl
                        border
                        bg-background/95
                        backdrop-blur-xl
                        p-2
                        shadow-2xl
                        data-[state=open]:animate-in
                        data-[state=closed]:animate-out
                        data-[state=closed]:fade-out-0
                        data-[state=open]:fade-in-0"
              >
                <DropdownMenuItem
                  className="
                        rounded-xl
                        cursor-pointer
                        py-3 "
                >
                  AI PDF Summary
                </DropdownMenuItem>

                <DropdownMenuItem
                  className="
                        rounded-xl
                        cursor-pointer
                        py-3 "
                >
                  Chat With PDF
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>

            {/* Features */}
            <a
              href="#tools"
              className="text-sm font-medium transition-colors hover:text-primary"
            >
              Features
            </a>

            {/* Pricing */}
            <a
              href="#why-docsaas"
              className="text-sm font-medium transition-colors hover:text-primary"
            >
              Pricing
            </a>

            {/* Resources */}
            <a
              href="#faq"
              className="text-sm font-medium transition-colors hover:text-primary"
            >
              Resources
            </a>

            {/* Contact */}
            <a
              href="#footer"
              className="text-sm font-medium transition-colors hover:text-primary"
            >
              Contact
            </a>
          </nav>

          {/* Right Side */}
          <div className="flex items-center gap-3">
            {/* AI Badge */}
            <div className="hidden md:flex items-center gap-2 rounded-full border px-3 py-1 text-xs">
              <Sparkles size={14} />
              AI Powered
            </div>

            {/* Theme Toggle  */}
            <HeaderThemeToggle />
            {/* Login */}
            {/* <Button variant="ghost" asChild className="hidden md:flex">
              <Link to="/login">Login</Link>
            </Button> */}

            {/* LOGIN */}
            <Button asChild a size="lg"
            className="hidden lg:flex px-6" >
                <Link to="/login">
                    Login
                </Link>
                </Button>

            {/* Mobile Menu */}
            <Button
              size="icon"
              variant="ghost"
              className="lg:hidden"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            >
              {mobileMenuOpen ? <X size={20} /> : <Menu size={20} />}
            </Button>
          </div>
        </div>
      </header>

        
      <div 
      className={`
                fixed
                top-0
                right-0
                z-1001
                h-screen
                w-[85vw] max-w-95
                border-l
                bg-background
                shadow-2xl
                transition-transform
                duration-300
                ease-in-out
                ${mobileMenuOpen ? "translate-x-0" : "translate-x-full"}
            `}
      >
        <div className="flex h-18 items-center justify-between border-b px-6">
          <Link to="/" className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-primary to-purple-600 text-white shadow-lg">
              <FileText size={20} />
            </div>

            <div>
              <h2 className="text-xl font-bold">DocSaaS</h2>

              <p className="text-xs text-muted-foreground">
                AI Document Workspace
              </p>
            </div>
          </Link>

          <Button
            size="icon"
            variant="ghost"
            onClick={() => setMobileMenuOpen(false)}
          >
            <X size={20} />
          </Button>
        </div>

        <div className="flex flex-col px-6 py-8">
          <a
            href="#tools"
            className=" rounded-xl px-4 py-4 text-basefont-medium transition-colors hover:bg-accent "
            onClick={() => setMobileMenuOpen(false)}
          >
            Tools
          </a>

          <a
            href="#ai-features"
            className=" rounded-xl px-4 py-4 text-basefont-medium transition-colors hover:bg-accent "
            onClick={() => setMobileMenuOpen(false)}
          >
            AI Features
          </a>

          <a
            href="#why-docsaas"
            className=" rounded-xl px-4 py-4 text-basefont-medium transition-colors hover:bg-accent "
            onClick={() => setMobileMenuOpen(false)}
          >
            Why DocSaaS
          </a>

          <a
            href="#faq"
            className=" rounded-xl px-4 py-4 text-basefont-medium transition-colors hover:bg-accent "
            onClick={() => setMobileMenuOpen(false)}
          >
            FAQ
          </a>

          <a
            href="#footer"
            className=" rounded-xl px-4 py-4 text-basefont-medium transition-colors hover:bg-accent "
            onClick={() => setMobileMenuOpen(false)}
          >
            Contact
          </a>

          <div className="mt-6 border-t pt-6">
            <Button
              variant="secondary"
              size="lg"
              className="mb-3 w-full"
              asChild
            >
              <Link to="/login">Login</Link>
            </Button>

            <Button className="w-full" asChild>
              <Link to="/register">Start Free</Link>
            </Button>
          </div>
        </div>
      </div>
    </>
  );
};

export default Header;
