import { Link } from "react-router-dom";
import { ShieldCheck } from "lucide-react";

import Container from "@/components/common/Container";
import Logo from "@/components/common/Logo";
import { IMAGE_TOOLS, PDF_TOOLS, RETENTION_MINUTES } from "@/features/home/constants/tools";

const LinkColumn = ({ title, links }) => (
  <div>
    <h3 className="mb-3 text-sm font-semibold">{title}</h3>
    <ul className="space-y-2 text-sm text-muted-foreground">
      {links.map((link) => (
        <li key={link.to}>
          <Link to={link.to} className="transition-colors hover:text-foreground">
            {link.label}
          </Link>
        </li>
      ))}
    </ul>
  </div>
);

const toolLinks = (tools) =>
  tools.filter((t) => t.available).map((t) => ({ to: `/tools/${t.id}`, label: t.title }));

const Footer = () => (
  <footer id="footer" className="pb-6 pt-16">
    <Container>
      <div className="glass rounded-3xl p-8 md:p-10">
        <div className="grid gap-10 md:grid-cols-2 lg:grid-cols-5">
          <div className="lg:col-span-2">
            <Logo />
            <p className="mt-4 max-w-xs text-sm text-muted-foreground">
              Free, fast PDF and image tools that work right in your browser. No sign-up needed.
            </p>
            <p className="mt-5 inline-flex items-center gap-2 rounded-full bg-accent px-3 py-1.5 text-xs text-accent-foreground">
              <ShieldCheck size={14} />
              Files auto-delete after {RETENTION_MINUTES} minutes
            </p>
          </div>

          <LinkColumn title="PDF tools" links={toolLinks(PDF_TOOLS)} />
          <LinkColumn title="Image tools" links={toolLinks(IMAGE_TOOLS)} />
          <LinkColumn
            title="Account"
            links={[
              { to: "/login", label: "Log in" },
              { to: "/register", label: "Create account" },
              { to: "/credits", label: "Credits" },
            ]}
          />
        </div>

        <div className="mt-10 flex flex-col items-center justify-between gap-2 border-t pt-6 text-xs text-muted-foreground sm:flex-row">
          <p>© {new Date().getFullYear()} DocSaaS. All rights reserved.</p>
          <p>Made with care by Kundan Kumar</p>
        </div>
      </div>
    </Container>
  </footer>
);

export default Footer;
