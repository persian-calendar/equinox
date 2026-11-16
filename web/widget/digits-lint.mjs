import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { runInNewContext } from "node:vm";

// Flatten circular arcs to within 0.01 grid units; this checks centerlines, not stroke outlines.
const tolerance = 0.01;
const tau = 2 * Math.PI;
const distance = (a, b) => Math.hypot(a.x - b.x, a.y - b.y);
const cross = (a, b) => a.x * b.y - a.y * b.x;
const subtract = (a, b) => ({ x: a.x - b.x, y: a.y - b.y });

function arcPoints(from, to, radius, large, clockwise) {
    if (!Number.isFinite(radius) || radius <= 0 ||
        !Number.isFinite(to.x) || !Number.isFinite(to.y)) {
        throw new Error("Invalid circular arc");
    }
    const dx = (to.x - from.x) / 2;
    const dy = (to.y - from.y) / 2;
    const halfChord = Math.hypot(dx, dy);
    if (!halfChord || halfChord > radius + 1e-8) {
        throw new Error("Invalid circular arc");
    }
    const h = Math.sqrt(Math.max(0, radius * radius - halfChord * halfChord));
    const sign = large === clockwise ? -1 : 1;
    const center = {
        x: (from.x + to.x) / 2 - sign * dy * h / halfChord,
        y: (from.y + to.y) / 2 + sign * dx * h / halfChord,
    };
    const start = Math.atan2(from.y - center.y, from.x - center.x);
    const end = Math.atan2(to.y - center.y, to.x - center.x);
    let sweep = (end - start + tau) % tau;
    if (!clockwise) sweep -= tau;
    const count = Math.max(1, Math.ceil(Math.abs(sweep) * Math.sqrt(radius / (8 * tolerance))));
    const points = [from];
    for (let i = 1; i < count; i++) {
        const angle = start + sweep * i / count;
        points.push({ x: center.x + radius * Math.cos(angle), y: center.y + radius * Math.sin(angle) });
    }
    points.push(to);
    return points;
}

export function primitives(commands) {
    const tokens = commands.flat(Infinity);
    const result = [];
    let position, subpath;
    for (let i = 0; i < tokens.length;) {
        const op = tokens[i++];
        if (op === "M") {
            position = { x: tokens[i++], y: tokens[i++] };
            if (!Number.isFinite(position.x) || !Number.isFinite(position.y)) {
                throw new Error("Invalid coordinates for M");
            }
            subpath = position;
            continue;
        }
        if (!position) throw new Error("Path must start with M");
        let next, points;
        if (op === "l") {
            next = { x: position.x + tokens[i++], y: position.y + tokens[i++] };
        } else if (op === "L") {
            next = { x: tokens[i++], y: tokens[i++] };
        } else if (op === "z" || op === "Z") {
            next = subpath;
        } else if (op === "a") {
            const rx = tokens[i++], ry = tokens[i++], rotation = tokens[i++];
            const large = tokens[i++], clockwise = tokens[i++];
            next = { x: position.x + tokens[i++], y: position.y + tokens[i++] };
            if (rx !== ry || rotation !== 0 || ![0, 1].includes(large) || ![0, 1].includes(clockwise)) {
                throw new Error("Only circular, unrotated arcs are supported");
            }
            points = arcPoints(position, next, rx, large, clockwise);
        } else {
            throw new Error(`Unsupported path command: ${op}`);
        }
        if (!Number.isFinite(next.x) || !Number.isFinite(next.y)) {
            throw new Error(`Invalid coordinates for ${op}`);
        }
        if (distance(position, next) > 1e-8) {
            result.push(points ?? [position, next]);
        }
        position = next;
    }
    return result;
}

export function intersection(a, b, c, d) {
    const r = subtract(b, a), s = subtract(d, c), q = subtract(c, a);
    const denominator = cross(r, s);
    if (Math.abs(denominator) < 1e-12) {
        if (Math.abs(cross(q, r)) > 1e-9) return null;
        const length = r.x * r.x + r.y * r.y;
        const projection = p => ((p.x - a.x) * r.x + (p.y - a.y) * r.y) / length;
        const start = Math.max(0, Math.min(projection(c), projection(d)));
        const end = Math.min(1, Math.max(projection(c), projection(d)));
        if (end < start) return null;
        const t = (start + end) / 2;
        return { x: a.x + t * r.x, y: a.y + t * r.y };
    }
    const t = cross(q, s) / denominator, u = cross(q, r) / denominator;
    if (t < -1e-9 || t > 1 + 1e-9 || u < -1e-9 || u > 1 + 1e-9) return null;
    return { x: a.x + t * r.x, y: a.y + t * r.y };
}

export function crossings(commands) {
    const parts = primitives(commands);
    const found = [];
    for (let i = 0; i < parts.length; i++) {
        for (let j = i + 1; j < parts.length; j++) {
            const a = parts[i], b = parts[j];
            for (let x = 1; x < a.length; x++) {
                for (let y = 1; y < b.length; y++) {
                    const point = intersection(a[x - 1], a[x], b[y - 1], b[y]);
                    if (!point) continue;
                    const atEndpoint = (part) =>
                        distance(point, part[0]) < tolerance * 2 ||
                        distance(point, part[part.length - 1]) < tolerance * 2;
                    if (atEndpoint(a) && atEndpoint(b)) continue;
                    if (!found.some(other => distance(other, point) < tolerance * 2)) found.push(point);
                }
            }
        }
    }
    return found;
}

export function loadDesign() {
    const html = readFileSync(new URL("digits.html", import.meta.url), "utf8");
    const script = html.match(/<script type="text\/javascript"><!\[CDATA\[([\s\S]*?)\]\]><\/script>/)?.[1];
    const marker = "// Render";
    if (!script || !script.includes(marker)) throw new Error("Digit definitions not found");
    return runInNewContext(
        script.slice(0, script.indexOf(marker)) + "\n({ arabic, persian, W, H })",
        {},
        { timeout: 1000 },
    );
}

// These are deliberate crossbars and joins, not self-crossing curves.
const joins = {
    "arabic 1": [{ x: 40, y: 90 }],
    "arabic 4": [{ x: 43, y: 75 }],
    "persian ۴": [{ x: 34.23, y: 44.26 }, { x: 42.42, y: 50.05 }],
};

export function unexpectedCrossings(alphabet, digit, path) {
    return crossings(path).filter(point =>
        !(joins[`${alphabet} ${digit}`] ?? []).some(join => distance(point, join) < 0.1),
    );
}

function main() {
    const { arabic, persian } = loadDesign();
    const alphabets = { arabic, persian };
    let errors = 0;
    for (const [alphabet, digits] of Object.entries(alphabets)) {
        for (const [digit, path] of Object.entries(digits)) {
            for (const point of unexpectedCrossings(alphabet, digit, path)) {
                console.error(`${alphabet} ${digit}: crossing at (${point.x.toFixed(2)}, ${point.y.toFixed(2)})`);
                errors++;
            }
        }
    }
    if (errors) process.exitCode = 1;
    else console.log("Digit paths: no unexpected centerline crossings");
}

if (process.argv[1] === fileURLToPath(import.meta.url)) main();
