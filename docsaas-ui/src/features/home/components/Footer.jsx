import { Link } from "react-router-dom";
import { FileText } from "lucide-react";
import {
  FaGithub,
  FaLinkedin,
  FaXTwitter,
} from "react-icons/fa6";
import { MdEmail } from "react-icons/md";

const Footer = () => {
  return (
    <footer className="relative overflow-hidden border-t border-primary/10">
      <div className="absolute inset-0 -z-10 overflow-hidden pointer-events-none">
        <div className="absolute left-[20%] top-0 h-[300px] w-[300px] rounded-full bg-primary/5 blur-[120px]" />
        <div className="absolute right-[20%] bottom-0 h-[300px] w-[300px] rounded-full bg-violet-500/5 blur-[120px]" />
      </div>

      <div className="container mx-auto px-5 py-16 md:px-6">
        <div className="grid gap-12 md:grid-cols-2 lg:grid-cols-5">
          {/* ABOUT */}
          <div className="lg:col-span-2">
            <Link
              to="/"
              className="flex items-center gap-3"
            >
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-primary to-purple-500 text-white shadow-[0_0_30px_rgba(139,92,246,0.35)]">
                <FileText size={22} />
              </div>

              <div>
                <h3 className="text-xl font-bold">DocSaaS</h3>

                <p className="text-sm text-muted-foreground">
                  AI Document Workspace
                </p>
              </div>
            </Link>

            <div className="mt-5">
              <h4 className="text-lg font-semibold">
                👋 Hi, I'm Kundan Kumar
              </h4>

              <p className="mt-3 max-w-md text-sm leading-relaxed text-muted-foreground">
                Full Stack Developer and Founder of DocSaaS. I enjoy
                building scalable software, AI-powered products and
                digital solutions that solve real-world problems.
                DocSaaS was created to make document processing
                smarter, faster and accessible for everyone.
              </p>

              {/* <p className="mt-3 text-sm text-muted-foreground">
                Passionate about Java, Spring Boot, React, AI and
                creating products that people genuinely love using.
              </p> */}

              <div className="mt-5 rounded-2xl border border-primary/10 bg-card/40 p-4 backdrop-blur-sm">
                <p className="text-sm font-medium">
                  Made with ❤️ by Kundan Kumar
                </p>

                <p className="mt-1 text-xs text-muted-foreground">
                  Building the future of AI-powered document workflows.
                </p>
              </div>
            </div>
            <div className="mt-6 flex gap-3">
  <a
    href="https://github.com/yourusername"
    target="_blank"
    rel="noreferrer"
    className="flex h-10 w-10 items-center justify-center rounded-xl border border-primary/10 bg-card/50 transition-all duration-300 hover:border-primary/30 hover:text-primary"
  >
    <FaGithub size={18} />
  </a>

  <a
    href="https://linkedin.com/in/yourusername"
    target="_blank"
    rel="noreferrer"
    className="flex h-10 w-10 items-center justify-center rounded-xl border border-primary/10 bg-card/50 transition-all duration-300 hover:border-primary/30 hover:text-primary"
  >
    <FaLinkedin size={18} />
  </a>

  <a
    href="https://x.com/yourusername"
    target="_blank"
    rel="noreferrer"
    className="flex h-10 w-10 items-center justify-center rounded-xl border border-primary/10 bg-card/50 transition-all duration-300 hover:border-primary/30 hover:text-primary"
  >
    <FaXTwitter size={18} />
  </a>

  <a
    href="mailto:your@email.com"
    className="flex h-10 w-10 items-center justify-center rounded-xl border border-primary/10 bg-card/50 transition-all duration-300 hover:border-primary/30 hover:text-primary"
  >
    <MdEmail size={18} />
  </a>
</div>
            
          </div>


          {/* AI TOOLS */}
          <div>
            <h4 className="mb-4 font-semibold">
              AI Tools
            </h4>

            <ul className="space-y-3 text-sm text-muted-foreground">
              <li><Link to="#">AI Summary</Link></li>
              <li><Link to="#">Chat With PDF</Link></li>
              <li><Link to="#">OCR</Link></li>
              <li><Link to="#">Image To Text</Link></li>
            </ul>
          </div>

          
          {/* PDF TOOLS */}
          <div>
            <h4 className="mb-4 font-semibold">
              Tools
            </h4>

            <ul className="space-y-3 text-sm text-muted-foreground">
                <li><Link to="/tools/merge-pdf">Merge PDF</Link></li>
                <li><Link to="/tools/split-pdf">Split PDF</Link></li>
                <li><Link to="/tools/compress-pdf">Compress PDF</Link></li>
                <li><Link to="/tools/pdf-to-word">PDF to Word</Link></li>
                <li><Link to="/tools/word-to-pdf">Word to PDF</Link></li>
                <li><Link to="/tools/pdf-to-images">PDF to Images</Link></li>
                <li><Link to="/tools/images-to-pdf">Images to PDF</Link></li>
                <li><Link to="/tools/scanned-pdf-ocr">Scanned PDF OCR</Link></li>
                <li><Link to="/tools/compress-image">Compress Image</Link></li>
                <li><Link to="/tools/jpg-to-png">JPG to PNG</Link></li>
                <li><Link to="/tools/png-to-jpg">PNG to JPG</Link></li>
            </ul>
          </div>

         

          {/* COMPANY */}
          <div>
            <h4 className="mb-4 font-semibold">
              Pages
            </h4>

            <ul className="space-y-3 text-sm text-muted-foreground">
              <li><Link to="/about">About</Link></li>
              <li><Link to="/contact">Contact</Link></li>
              <li><Link to="/login">Login</Link></li>
              <li><Link to="/register">Register</Link></li>
            </ul>
          </div>
        </div>

        <div className="mt-14 flex flex-col items-center justify-between gap-4 border-t border-primary/10 pt-8 text-sm text-muted-foreground md:flex-row">
          <p>
            © {new Date().getFullYear()} | DocSaaS. All rights reserved.
          </p>

          <div className="flex gap-6">
            <Link to="/privacy-policy">
              Privacy Policy
            </Link>

            <Link to="/terms-of-service">
              Terms of Service
            </Link>
          </div>
        </div>
      </div>
    </footer>
  );
};

export default Footer;