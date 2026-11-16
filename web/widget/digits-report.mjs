import { mkdirSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { loadDesign } from "./digits-lint.mjs";
import { measureDesign } from "./digits-metrics.mjs";

const report = measureDesign(loadDesign());
const data = JSON.stringify(report).replace(/</g, "\\u003c");
const output = new URL("dist/digits-report.html", import.meta.url);
mkdirSync(new URL("dist/", import.meta.url), { recursive: true });

const html = `<!doctype html>
<html lang="en">
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Persian digit measurements</title>
<style>
:root { color-scheme: dark; font-family: system-ui, sans-serif; background: #0c151e; color: #eef4fa; }
body { max-width: 1150px; margin: auto; padding: 20px; }
h1 { margin-bottom: 4px; } h2 { margin-top: 32px; }
p, li { line-height: 1.5; } a { color: #82c8ff; }
button, select { font: inherit; color: inherit; background: #233848; border: 1px solid #7996a8; border-radius: 4px; padding: 5px 8px; }
button { cursor: pointer; } button:focus-visible, select:focus-visible { outline: 2px solid #8ce2b0; }
.notes { color: #bfced9; }
.scroll { overflow-x: auto; }
table { border-collapse: collapse; font-variant-numeric: tabular-nums; }
th, td { padding: 7px; border-bottom: 1px solid #3c5161; text-align: right; white-space: nowrap; }
th { color: #bfced9; } .glyph { display: inline-block; vertical-align: middle; width: 30px; height: 45px; }
svg { fill: none; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; }
.proof { direction: ltr; display: flex; flex-wrap: wrap; gap: 12px 24px; }
.proof-group { direction: ltr; white-space: nowrap; max-width: 100%; overflow-x: auto; }
.proof-group svg { width: 36px; height: 54px; vertical-align: middle; }
.pair-table th, .pair-table td { text-align: center; padding: 2px; }
.pair-table button { min-width: 50px; border: 1px solid transparent; }
.pair-table button[aria-pressed="true"] { border-color: #e8f8ff; }
.pair-detail { display: flex; align-items: center; flex-wrap: wrap; gap: 20px; min-height: 80px; }
.pair-detail svg { width: 56px; height: 84px; }
@media (max-width: 600px) { body { padding: 12px; } .pair-table button { min-width: 44px; } }
</style>
<h1>Persian digit measurements</h1>
<p class="notes">Baseline only: the outlines have not been optimized. <a href="../digits.html">Open the digit study</a>.</p>
<label for="weight">Stroke width in grid units: </label><select id="weight"></select>
<p class="notes" id="settings"></p>
<h2>Visual proof</h2>
<div class="proof" id="proof" aria-label="Persian digit proof"></div>
<h2>Individual digits</h2>
<div class="scroll"><table>
<thead><tr><th>Digit</th><th>Ink area</th><th>Ink centroid (x, y)</th><th>Centerline bounds (L, T, R, B)</th><th>Ink bounds (L, T, R, B)</th><th>Non-joined distance</th><th>Crossings</th><th>Clipped</th></tr></thead>
<tbody id="glyphs"></tbody></table></div>
<h2>All 100 ordered pairs</h2>
<p class="notes">Cells show mean horizontal gap in grid units across rows where both digits have ink. Select a pair for its minimum, maximum and shared-row count. Darker cells indicate smaller means; this is a measurement, not a quality score.</p>
<div id="pair-detail" class="pair-detail" aria-live="polite"></div>
<div class="scroll"><table class="pair-table" id="pairs"></table></div>
<h2>Method and limits</h2>
<ul class="notes">
<li>Each digit uses the same ${report.width} × ${report.height} cell. Shapes are unchanged; pairs read left to right with zero additional gap.</li>
<li>Round-capped strokes are sampled at ${report.step} grid-unit intervals. Ink area, bounds and centroid are estimates in the cell; clipped strokes are flagged.</li>
<li>Gaps compare the outermost ink on each shared sample row. This ignores counters and rows without ink in both digits; a dash means there are no shared rows.</li>
<li>Non-joined distance is the minimum centerline distance between path primitives without shared endpoints. It includes intentional crossings and excludes entire pairs that share endpoints, so it is not a full stroke-clearance test. Subtracting the stroke width is only a proximity indicator.</li>
<li>Crossings use the existing centerline lint and its documented intentional joins. It approximates circular arcs and does not check all thick-stroke overlaps.</li>
</ul>
<script>
const data = ${data};
const ns = "http://www.w3.org/2000/svg";
const weight = document.getElementById("weight");
const format = n => n == null ? "—" : n.toFixed(1);
let selected = [0, 0];
for (const item of data.weights) {
    const option = document.createElement("option");
    option.value = item.weight;
    option.textContent = item.weight;
    weight.append(option);
}
function glyph(index, size) {
    const svg = document.createElementNS(ns, "svg");
    svg.setAttribute("viewBox", "0 0 " + data.width + " " + data.height);
    svg.setAttribute("aria-hidden", "true");
    if (size === "small") svg.classList.add("glyph");
    const path = document.createElementNS(ns, "path");
    path.setAttribute("d", data.paths[index]);
    path.setAttribute("stroke-width", weight.value);
    svg.append(path);
    return svg;
}
function textCell(row, text, tag = "td") {
    const cell = document.createElement(tag);
    cell.textContent = text;
    row.append(cell);
    return cell;
}
function render() {
    const measurement = data.weights.find(item => String(item.weight) === weight.value);
    if (!measurement) throw new Error("Unknown stroke weight");
    document.getElementById("settings").textContent =
        "Cell: " + data.width + " × " + data.height + " · sampling: " + data.step +
        " · weight: " + measurement.weight;
    const proof = document.getElementById("proof");
    proof.replaceChildren();
    for (const sample of ["۰۱۲۳۴۵۶۷۸۹", "۰۰ ۱۱ ۱۴ ۴۹ ۶۵", "۲۲ ۳۳ ۵۵ ۶۶ ۹۹", "۳۶۵ ۰۴ ۳۹ ۵۸"]) {
        const group = document.createElement("div");
        group.className = "proof-group";
        group.setAttribute("aria-label", sample);
        for (const digit of sample) {
            if (digit === " ") group.append(document.createTextNode("  "));
            else group.append(glyph(data.digits.indexOf(digit)));
        }
        proof.append(group);
    }
    const body = document.getElementById("glyphs");
    body.replaceChildren();
    for (let i = 0; i < data.digits.length; i++) {
        const m = measurement.glyphs[i], row = document.createElement("tr");
        const name = textCell(row, data.digits[i] + " ", "th");
        name.scope = "row";
        name.append(glyph(i, "small"));
        textCell(row, format(m.area));
        textCell(row, format(m.centroid.x) + ", " + format(m.centroid.y));
        for (const bounds of [m.centerlineBounds, m.inkBounds]) {
            textCell(row, [bounds.left, bounds.top, bounds.right, bounds.bottom].map(format).join(", "));
        }
        textCell(row, m.nonJoinedDistance == null ? "—" :
            format(m.nonJoinedDistance) + " (− width: " + format(m.nonJoinedDistance - measurement.weight) + ")");
        textCell(row, String(data.crossings[i].length));
        textCell(row, m.clipped ? "yes" : "no");
        body.append(row);
    }
    const table = document.getElementById("pairs");
    table.replaceChildren();
    const values = measurement.pairs.flat().filter(Boolean).map(pair => pair.mean);
    const low = Math.min(...values), high = Math.max(...values);
    const header = document.createElement("tr");
    textCell(header, "L → R", "th");
    for (const digit of data.digits) {
        const cell = textCell(header, digit, "th");
        cell.scope = "col";
    }
    table.append(header);
    measurement.pairs.forEach((pairs, i) => {
        const row = document.createElement("tr");
        const label = textCell(row, data.digits[i], "th");
        label.scope = "row";
        pairs.forEach((pair, j) => {
            const cell = document.createElement("td"), button = document.createElement("button");
            button.textContent = pair ? format(pair.mean) : "—";
            button.setAttribute("aria-label", data.digits[i] + " then " + data.digits[j] +
                ": " + (pair ? "mean gap " + format(pair.mean) : "no shared ink rows"));
            button.setAttribute("aria-pressed", String(selected[0] === i && selected[1] === j));
            if (pair) {
                const lightness = high === low ? 32 : 22 + 18 * (pair.mean - low) / (high - low);
                button.style.backgroundColor = "hsl(205 38% " + lightness + "%)";
            }
            button.addEventListener("click", () => {
                selected = [i, j];
                render();
                table.querySelectorAll("button")[i * data.digits.length + j].focus({ preventScroll: true });
            });
            cell.append(button);
            row.append(cell);
        });
        table.append(row);
    });
    const detail = document.getElementById("pair-detail");
    detail.replaceChildren();
    const image = document.createElement("div");
    image.setAttribute("aria-hidden", "true");
    image.append(glyph(selected[0]), glyph(selected[1]));
    const description = document.createElement("p");
    const pair = measurement.pairs[selected[0]][selected[1]];
    description.textContent = data.digits[selected[0]] + " → " + data.digits[selected[1]] +
        (pair ? ": mean " + format(pair.mean) + ", min " + format(pair.min) +
            ", max " + format(pair.max) + ", shared rows " + pair.rows : ": no shared ink rows");
    detail.append(image, description);
}
weight.addEventListener("change", render);
render();
</script>
</html>`;
writeFileSync(output, html);
console.log(`Wrote ${fileURLToPath(output)}`);
