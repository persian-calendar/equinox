import { spawnSync } from "node:child_process";
import { existsSync, readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { resolve } from "node:path";
import { deflateSync } from "node:zlib";

const root = fileURLToPath(new URL(".", import.meta.url));
const file = resolve(root, process.argv[2] ?? "dist/index.html");
const sandbox = resolve(root, ".sandbox");
const html = readFileSync(file);
const compressed = deflateSync(html, { level: 9 }).length;
console.log(`HTML: ${html.length}/32768 bytes; zlib: ${compressed}/4096 bytes`);
if (html.length > 32768 || compressed > 4096) {
    throw new Error("Telegram widget size limit exceeded");
}
if (!existsSync(resolve(sandbox, "packages/core/scripts/sandbox-node.ts"))) {
    throw new Error("Telegram sandbox is not installed; see web/widget/README.md");
}

const result = spawnSync(
    "bun",
    [resolve(sandbox, "packages/core/scripts/sandbox-node.ts"), file, "--json"],
    { cwd: sandbox, encoding: "utf8" },
);
if (result.error) throw result.error;
if (result.status !== 0) {
    throw new Error(`Telegram sandbox failed:\n${result.stderr || result.stdout}`);
}

const report = JSON.parse(result.stdout);
const errors = report.logs.filter((log) => log.level === "error");
if (report.diags.length || errors.length || report.stopped || report.throttled) {
    throw new Error(
        `Telegram sandbox reported problems:\n${JSON.stringify(
            {
                diagnostics: report.diags,
                errors,
                stopped: report.stopped,
                throttled: report.throttled,
            },
            null,
            2,
        )}`,
    );
}
console.log("Telegram sandbox: no diagnostics or runtime errors");
