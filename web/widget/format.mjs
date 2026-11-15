import { readFile, writeFile } from "node:fs/promises";
import { format } from "prettier";

const file = new URL("index.html", import.meta.url);
const html = await format(await readFile(file, "utf8"), {
    parser: "html",
    tabWidth: 4,
});
let blocks = 0;
const result = html.replace(/^<(style|script)>\n([\s\S]*?)^<\/\1>/gm, (_, tag, content) => {
    blocks++;
    return `<${tag}>\n${content.replace(/^ {4}/gm, "")}</${tag}>`;
});
if (blocks !== 2) throw new Error("Expected one top-level style and script block");
await writeFile(file, result);
