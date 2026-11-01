/**
 * Fails fast when the code imports a package that is not declared in package.json
 * or not installed in node_modules (e.g. added to package.json by hand without `npm install`).
 * Runs automatically before `npm run dev` and `npm run build`.
 */
import { readFileSync, readdirSync, existsSync, statSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { builtinModules } from "node:module";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const pkg = JSON.parse(readFileSync(join(root, "package.json"), "utf8"));
const declared = { ...pkg.dependencies, ...pkg.devDependencies };
const builtins = new Set([...builtinModules, ...builtinModules.map((m) => `node:${m}`)]);

const files = [];
const walk = (dir) => {
  for (const name of readdirSync(dir)) {
    const p = join(dir, name);
    if (statSync(p).isDirectory()) walk(p);
    else if (/\.(m?jsx?|tsx?)$/.test(name)) files.push(p);
  }
};
walk(join(root, "src"));
for (const f of ["vite.config.js", "eslint.config.js"]) if (existsSync(join(root, f))) files.push(join(root, f));

const importRe = /(?:import|export)\s[^'"]*?from\s*["']([^"']+)["']|import\s*\(\s*["']([^"']+)["']\s*\)|import\s+["']([^"']+)["']/g;
const problems = [];

for (const file of files) {
  const src = readFileSync(file, "utf8");
  for (const m of src.matchAll(importRe)) {
    const spec = (m[1] || m[2] || m[3]).split("?")[0];
    if (spec.startsWith(".") || spec.startsWith("/") || spec.startsWith("@/") || spec.startsWith("virtual:") || builtins.has(spec)) continue;
    const parts = spec.split("/");
    const name = spec.startsWith("@") ? parts.slice(0, 2).join("/") : parts[0];
    const where = file.slice(root.length + 1);
    if (!declared[name]) { problems.push(`${where}: "${name}" is imported but not in package.json`); continue; }
    const pkgDir = join(root, "node_modules", name);
    if (!existsSync(join(pkgDir, "package.json"))) { problems.push(`${where}: "${name}" is in package.json but not installed`); continue; }
    // Deep file imports (e.g. pdfjs-dist/build/pdf.worker.min.mjs) must exist too.
    const sub = parts.slice(spec.startsWith("@") ? 2 : 1).join("/");
    if (sub && /\.[a-z]+$/i.test(sub) && !existsSync(join(pkgDir, sub))) problems.push(`${where}: file "${spec}" does not exist in the installed package`);
  }
}

if (problems.length) {
  console.error("\nDependency check failed:\n  " + [...new Set(problems)].join("\n  ") + "\n\nFix: run `npm install` (or `npm install <package>`), then try again.\n");
  process.exit(1);
}
console.log(`Dependency check passed (${files.length} files).`);
