import assert from "node:assert/strict";
import test from "node:test";
import { loadDesign } from "./digits-lint.mjs";
import { measureDesign, measureGlyph, measurePair } from "./digits-metrics.mjs";

test("measures stroke footprint and reports clipping", () => {
    const line = ["M", 20, 20, "L", 20, 80];
    const thin = measureGlyph(line, 80, 120, 2);
    const thick = measureGlyph(line, 80, 120, 4);
    assert.ok(Math.abs(thin.area - (120 + Math.PI)) < 2);
    assert.equal(thin.centroid.x, 20);
    assert.equal(thin.centroid.y, 50);
    assert.equal(thin.clipped, false);
    assert.ok(thick.area > thin.area);
    assert.equal(measureGlyph(["M", 0, 20, "L", 0, 80], 80, 120, 2).clipped, true);
});

test("circular ink area converges to the annulus area", () => {
    const circle = ["M", 40, 50, "a", 10, 10, 0, 0, 1, 0, 20, "a", 10, 10, 0, 0, 1, 0, -20];
    const measured = measureGlyph(circle, 80, 120, 2, 0.25);
    assert.ok(Math.abs(measured.area - 40 * Math.PI) < 2);
    assert.ok(Math.abs(measured.centroid.x - 40) < 0.05);
    assert.ok(Math.abs(measured.centroid.y - 60) < 0.05);
});

test("pair gaps are ordered, based on shared rows, and can be unavailable", () => {
    const left = measureGlyph(["M", 20, 20, "L", 20, 40], 80, 120, 2);
    const right = measureGlyph(["M", 30, 20, "L", 30, 40], 80, 120, 2);
    assert.equal(measurePair(left, right, 80).min, 88);
    assert.equal(measurePair(right, left, 80).min, 68);
    assert.ok(Math.abs(measurePair(left, right, 80).mean - 88) < 0.1);
    const separate = measureGlyph(["M", 30, 80, "L", 30, 100], 80, 120, 2);
    assert.equal(measurePair(left, separate, 80), null);
});

test("non-joined distance does not hide a crossing", () => {
    const path = ["M", 10, 10, "L", 30, 30, "M", 10, 30, "L", 30, 10];
    assert.equal(measureGlyph(path, 80, 120, 2).nonJoinedDistance, 0);
    const joined = ["M", 10, 10, "L", 30, 10, "L", 30, 30];
    assert.equal(measureGlyph(joined, 80, 120, 2).nonJoinedDistance, null);
});

test("baseline report covers every Persian digit and ordered pair", () => {
    const report = measureDesign(loadDesign(), [1.5, 4, 8], 0.5);
    assert.equal(report.digits.join(""), "۰۱۲۳۴۵۶۷۸۹");
    for (const weight of report.weights) {
        assert.equal(weight.glyphs.length, 10);
        assert.equal(weight.pairs.length, 10);
        assert.ok(weight.pairs.every(row => row.length === 10));
        assert.ok(weight.glyphs.every(glyph => glyph.area > 0 && !glyph.clipped));
        assert.ok(weight.pairs.flat().every(pair =>
            pair === null || (pair.min <= pair.mean && pair.mean <= pair.max && pair.rows > 0)));
    }
    assert.ok(report.crossings.every(crossings => crossings.length === 0));
});
