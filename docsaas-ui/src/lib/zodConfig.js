import { z } from "zod";

// Our Content-Security-Policy forbids eval. Tell zod before any schema exists so it never probes
// its eval-based fast path (which the browser would block and report). Imported first in main.jsx.
z.config({ jitless: true });
