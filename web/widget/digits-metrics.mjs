import { intersection, primitives, unexpectedCrossings } from "./digits-lint.mjs";

const distance = (a, b) => Math.hypot(a.x - b.x, a.y - b.y);

function pointSegmentDistance(p, a, b) {
    const dx = b.x - a.x, dy = b.y - a.y;
    const length = dx * dx + dy * dy;
    const t = length === 0 ? 0 : Math.max(0, Math.min(1, ((p.x - a.x) * dx + (p.y - a.y) * dy) / length));
    return Math.hypot(p.x - a.x - t * dx, p.y - a.y - t * dy);
}

function nonJoinedDistance(parts) {
    let minimum = Infinity;
    for (let i = 0; i < parts.length; i++) {
        for (let j = i + 1; j < parts.length; j++) {
            const a = parts[i], b = parts[j];
            const endpointsA = [a[0], a.at(-1)], endpointsB = [b[0], b.at(-1)];
            if (endpointsA.some(x => endpointsB.some(y => distance(x, y) < 0.02))) continue;
            for (let x = 1; x < a.length; x++) {
                for (let y = 1; y < b.length; y++) {
                    if (intersection(a[x - 1], a[x], b[y - 1], b[y])) return 0;
                    minimum = Math.min(minimum,
                        pointSegmentDistance(a[x - 1], b[y - 1], b[y]),
                        pointSegmentDistance(a[x], b[y - 1], b[y]),
                        pointSegmentDistance(b[y - 1], a[x - 1], a[x]),
                        pointSegmentDistance(b[y], a[x - 1], a[x]));
                }
            }
        }
    }
    return Number.isFinite(minimum) ? minimum : null;
}

// Sample round-capped strokes on a regular grid, taking the union at joins.
export function measureGlyph(path, width, height, weight, step = 0.5) {
    if (![width, height, weight, step].every(n => Number.isFinite(n) && n > 0) ||
        !Number.isInteger(width / step) || !Number.isInteger(height / step)) {
        throw new Error("Grid dimensions, weight and sampling step must be positive and compatible");
    }
    const parts = primitives(path);
    if (!parts.length) throw new Error("Cannot measure an empty glyph");
    const columns = width / step, rows = height / step, radius = weight / 2;
    const pixels = new Uint8Array(columns * rows);
    let centerLeft = Infinity, centerTop = Infinity, centerRight = -Infinity, centerBottom = -Infinity;
    for (const part of parts) {
        for (const p of part) {
            centerLeft = Math.min(centerLeft, p.x);
            centerTop = Math.min(centerTop, p.y);
            centerRight = Math.max(centerRight, p.x);
            centerBottom = Math.max(centerBottom, p.y);
        }
        for (let i = 1; i < part.length; i++) {
            const a = part[i - 1], b = part[i];
            const x0 = Math.max(0, Math.floor((Math.min(a.x, b.x) - radius) / step));
            const x1 = Math.min(columns - 1, Math.floor((Math.max(a.x, b.x) + radius) / step));
            const y0 = Math.max(0, Math.floor((Math.min(a.y, b.y) - radius) / step));
            const y1 = Math.min(rows - 1, Math.floor((Math.max(a.y, b.y) + radius) / step));
            for (let y = y0; y <= y1; y++) {
                for (let x = x0; x <= x1; x++) {
                    const p = { x: (x + 0.5) * step, y: (y + 0.5) * step };
                    if (pointSegmentDistance(p, a, b) <= radius) pixels[y * columns + x] = 1;
                }
            }
        }
    }
    let count = 0, sumX = 0, sumY = 0, left = width, top = height, right = 0, bottom = 0;
    const profile = Array.from({ length: rows }, () => null);
    for (let y = 0; y < rows; y++) {
        for (let x = 0; x < columns; x++) {
            if (!pixels[y * columns + x]) continue;
            count++;
            sumX += (x + 0.5) * step;
            sumY += (y + 0.5) * step;
            left = Math.min(left, x * step);
            right = Math.max(right, (x + 1) * step);
            top = Math.min(top, y * step);
            bottom = Math.max(bottom, (y + 1) * step);
            if (!profile[y]) profile[y] = { left: x * step, right: (x + 1) * step };
            else profile[y].right = (x + 1) * step;
        }
    }
    if (!count) throw new Error("Glyph has no sampled ink in the cell");
    return {
        area: count * step * step,
        centroid: { x: sumX / count, y: sumY / count },
        inkBounds: { left, top, right, bottom },
        centerlineBounds: { left: centerLeft, top: centerTop, right: centerRight, bottom: centerBottom },
        clipped: centerLeft - radius < 0 || centerTop - radius < 0 ||
            centerRight + radius > width || centerBottom + radius > height,
        nonJoinedDistance: nonJoinedDistance(parts),
        profile,
    };
}

export function measurePair(left, right, width) {
    if (left.profile.length !== right.profile.length) throw new Error("Incompatible glyph profiles");
    const gaps = [];
    for (let y = 0; y < left.profile.length; y++) {
        if (left.profile[y] && right.profile[y]) {
            gaps.push(width + right.profile[y].left - left.profile[y].right);
        }
    }
    if (!gaps.length) return null;
    const mean = gaps.reduce((sum, gap) => sum + gap, 0) / gaps.length;
    return {
        mean,
        min: Math.min(...gaps),
        max: Math.max(...gaps),
        rows: gaps.length,
    };
}

export function measureDesign(design, weights = [1.5, 4, 8], step = 0.5) {
    const glyphs = Object.entries(design.persian);
    if (glyphs.length !== 10) throw new Error("Expected ten Persian digits");
    return {
        width: design.W,
        height: design.H,
        step,
        digits: glyphs.map(([digit]) => digit),
        paths: glyphs.map(([, path]) => path.flat(Infinity).join(" ")),
        crossings: glyphs.map(([digit, path]) => unexpectedCrossings("persian", digit, path)),
        weights: weights.map(weight => {
            const measured = glyphs.map(([, path]) => measureGlyph(path, design.W, design.H, weight, step));
            return {
                weight,
                glyphs: measured.map(({ profile, ...metrics }) => metrics),
                pairs: measured.map(left => measured.map(right => measurePair(left, right, design.W))),
            };
        }),
    };
}
