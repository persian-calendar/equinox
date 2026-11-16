import assert from "node:assert/strict";
import test from "node:test";
import { crossings } from "./digits-lint.mjs";

const radians = Math.PI / 180;
const arc = (r, start, sweep) => [
    "a", r, r, 0, Math.abs(sweep) > 180 ? 1 : 0, sweep > 0 ? 1 : 0,
    r * (Math.cos((start + sweep) * radians) - Math.cos(start * radians)),
    r * (Math.sin((start + sweep) * radians) - Math.sin(start * radians)),
];
const stemRadius = 60 / (Math.sin(350.5 * radians) - Math.sin(-27.5 * radians));
const stem = (start, height, reflected = false) => {
    const end = Math.asin(Math.sin(start * radians) + height / stemRadius) / radians;
    return arc(stemRadius, reflected ? 180 - start : start, reflected ? start - end : end - start);
};

test("detects intersections and overlaps between strokes", () => {
    assert.equal(crossings(["M", 0, 0, "L", 10, 10, "M", 0, 10, "L", 10, 0]).length, 1);
    assert.equal(crossings(["M", 0, 0, "L", 10, 0, "M", 2, 0, "L", 8, 0]).length, 1);
});

test("allows ordinary shared endpoints and rejects unsupported curves", () => {
    assert.deepEqual(crossings(["M", 0, 0, "L", 10, 0, "L", 10, 10]), []);
    assert.throws(() => crossings(["M", 0, 0, "C", 1, 2, 3, 4, 5, 6]), /Unsupported/);
    assert.throws(() => crossings(["M", 0, 0, "a", -1, -1, 0, 0, 1, 1, 1]), /Invalid/);
});

test("detects the original ۲ bowl crossing", () => {
    const old = [["M", 55, 30], arc(15, 0, 180), stem(-27.5, 60)];
    const fixed = [["M", 55, 30], arc(15, 0, 152.5), stem(-27.5, 60 - 15 * Math.sin(152.5 * radians))];
    assert.equal(crossings(old).length, 1);
    assert.deepEqual(crossings(fixed), []);
});

test("detects the original ۶ bowl crossing", () => {
    const make = end => {
        const y = 44 + 14 * Math.sin(end * radians);
        return [
            ["M", 42 + 14 * Math.cos(-60 * radians), 44 + 14 * Math.sin(-60 * radians)],
            arc(14, -60, end - 300),
            stem(-45, 90 - y, true),
        ];
    };
    assert.equal(crossings(make(25)).length, 1);
    assert.deepEqual(crossings(make(50)), []);
});
