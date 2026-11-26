# Constraint discovery for the Persian digits

Notes from analysing `digits.html`, `digits-lint.mjs` and `digits-metrics.mjs`.
Not a proposal to change the design — a map of what the current system *does* constrain, what it
merely *measures*, and what it cannot see at all.

---

## 1. What the system already is

A centerline DSL, not an SVG drawing:

- commands `M / L / l / a / z` only; arcs must be **circular and unrotated** (`rx !== ry` or
  `rotation !== 0` throws); curves are flattened to 0.01 grid units.
- cell **80 × 120**, sampling step 0.5, strokes are round-capped with weight swept over 1.5 / 4 / 8
  (the UI slider allows 0.7–8).
- measured per glyph: ink area, centroid, ink bounds, centerline bounds, clipping,
  `nonJoinedDistance`, plus a per-row horizontal profile used for pair gaps.
- linted: unexpected centerline crossings, with a hand-written allowlist of deliberate joins
  (`arabic 1`, `arabic 4`, `persian ۴`).

This is closer to a *proof system* than to drawing. That is the right instinct, and it is why the
generative-model question is mostly a distraction (see §6).

## 2. Measured, but nothing asserts it

I ran the existing metrics over the ten Persian digits. Lint passes: **0 unexpected crossings**.
But:

| quantity | w = 1.5 | w = 4 | w = 8 | asserted? |
|---|---|---|---|---|
| ink area range | 94 … 260 | 248 … 681 | 506 … 1359 | no |
| centroid y (mean) | 56.8 | 56.9 | 57.0 | no |
| centroid y spread | **15.4 units** | 14.9 | 14.6 | no |
| ink height spread | **40.5 units** | 40.0 | 40.0 | no |
| clipped | none | none | none | reported only |
| `nonJoinedDistance` | computed | computed | computed | no minimum |
| pair gaps | computed | computed | computed | no target |

Two of these are worth thinking about. The set's **centroid sits ~3 units above the cell centre**
(57 on a 120-tall cell) and the *spread across digits* is ~15 units — so a row of digits has no
shared optical centreline. And **ink height varies by 40 units** (۰ is a 21-unit dot; the rest span
60–80). Both may be deliberate. Neither is a rule, so neither is protected from drift.

## 3. Invisible to every check: the DSL has no straight line

Every "straight" stroke is a large-radius arc. ۱, ۷ and ۸ all use **r ≈ 202** in an 80 × 120 cell.
For ۱ (chord ≈ 63 units) the sagitta is `c² / 8R ≈ 2.5 units`.

So each nominally straight stroke **bows by about 2.5 units**, and no metric sees it. At w = 0.7 that
bow reads as calligraphic tension; at w = 8 it can read as a wobble. That is a design decision that
is currently *implicit* — it should be a number someone chose.

Similarly unmeasured: the angle at which two arcs meet (G1 continuity). A path can pass lint and
still have a visible kink, because the lint checks centerline crossings, not tangent continuity.

## 4. The blind spot of constraint-driven design

Your checks are **per-glyph, geometric, mass-based**. Typography is **set-level and optical**:

- what is right about a digit is judged in a row (`۳۶۵`), against its neighbours and against the
  other nine;
- apparent weight depends on *junctions*: a T-join of two strokes reads heavier than an isolated
  stroke of the same width, and your union sampler correctly avoids double-counting the pixel area,
  but the ink is not the perception;
- counters are a *state* that changes with weight. ۰ is `r = 10`, so its counter is `20 − w` — at
  w = 8 it is 12 units, fine, but only by luck. Nothing guards it.

The general lesson: **a model will optimise exactly what you assert and ignore the rest.** Constraint
discovery is therefore the real work, and it is a search problem, not a drawing problem.

## 5. A ladder of candidate rules, cheapest first

Ordered so each level is checkable with the code you already have. Add a rule only if a real design
variant would violate it while still looking acceptable.

| # | rule | needs | why it exists |
|---|---|---|---|
| R1 | no unintended crossings | have | topology |
| R2 | ink inside the cell at every weight | have (`clipped`) | containment |
| R3 | join tangents differ by < τ (e.g. 15°) | new: angle at shared endpoints | kills kinks lint can't see |
| R4 | max bow per stroke ≤ b, or declare a straightness intent | new: curvature | makes the r≈202 choice explicit |
| R5 | vertices on the 0.5 lattice, or on arc-derived points | new: coordinate audit | stops drift (e.g. 34.23, 44.644…) |
| R6 | set-level bands: centroid y, cap/baseline, ink height per glyph role | new: set invariants | optical alignment across ten digits |
| R7 | optical mass: ink area ÷ centerline length, normalised per weight, within a band | extend metrics | compares a 1-stroke glyph with an 8-stroke one |
| R8 | counter clearance ≥ c at w_max | new: distance between non-joined parts of the *same* closed subpath | ۰ survives w = 8 only by accident |
| R9 | pair-gap rhythm: max−min across a row ≤ g | have the data | kerning as a sequence, not a per-pair mean |
| R10 | weight family: no topology change for w ∈ [0.7, 8] | sample the whole sweep | the slider is public; the design must hold |

R1–R2 exist. R3–R8 are new measurements. R9 needs only an assertion. R10 is a loop over the range.

## 6. Where a model helps, and where it does not

**Does not help:** emitting glyph outlines. Your dialect rejects almost everything an SVG-tuned model
produces (`C`/`Q` Béziers, transforms, rotations, fills), and its training distribution is icons and
illustrations, not monoline calligraphic geometry. A general code model already writes this DSL
better — measured 193 tok/s locally on Qwen3-Coder-Next, with the file in context.

**Does help**, in this order:

1. **Writing the new checks** (§5 R3–R10) as pure functions over `primitives()` output — small,
   verifiable, testable against the current set.
2. **Proposing candidate invariants.** Give the model the metrics report and the rendered row, ask it
   for invariants that would distinguish a good variant from a bad one. Cheap, and it is exactly the
   "constraint discovery" step.
3. **Writing the search**, not the art: a parametric anchor model plus coordinate descent / random
   restarts scored by your metrics.
4. Vision, last: render a row to PNG and ask a VLM to critique balance. Treat as a weak prior only.

## 7. How to find the *minimal* rule set

The question "what is the minimal set of rules that designs these digits" is answerable by
experiment, not by taste:

1. **Build a variant corpus.** Perturb anchors by ±0.5/±1 units, sample the weight sweep, and keep a
   few hundred candidates. Each is only a few hundred bytes of DSL.
2. **Label them.** You accept/reject by eye — this is the only step that needs taste. A few dozen is
   enough to start; the current ten are a seed.
3. **Mine rules that reproduce the labels.** Decision-tree/rule induction over the metric vector
   (area, centroid, bow, join angles, counter clearance, pair-gap spread, …). Anything that predicts
   your labels is a candidate invariant; anything that never fires on rejected variants is dead
   weight.
4. **Minimise.** Drop rules one at a time; keep a rule only if removing it lets a rejected variant
   through. What survives is a minimal set *relative to your corpus* — which is the honest definition.
5. **Re-run when the corpus grows.** The rules are a hypothesis about your taste, not a law.

The elegant outcome would be something like: *"ten digits are fully determined by (topology graph) +
(bow/straightness) + (centroid band) + (counter clearance at max weight)"* — a generative grammar, not
a pile of paths.

## 8. Concrete next steps

- `digits-invariants.mjs` — implement R3, R4, R5, R7, R8, R9, R10 as pure functions returning
  violations, in the style of `unexpectedCrossings()`; add `digits-invariants.test.mjs`.
- `digits-bow.mjs` — sagitta and join-angle per subpath, printed as a table; it turns the r ≈ 202
  decision into a visible number.
- `digits-sweep.mjs` — run lint + metrics + invariants across the whole 0.7–8 weight range and report
  the first weight at which each rule fails.
- Later: the anchor parameterisation and the search, once the invariant set is stable.

## 9. Open questions

- Is the 3-unit offset of the centroid above cell centre intentional (descender space), or drift?
- Is a 40-unit ink-height spread across the set intended, or should ۰ be optically aligned to the
  others even though its shape is a dot?
- Should ۱/۷/۸ be truly straight (line primitive, or r = ∞) or is the 2.5-unit bow part of the voice?
- Does the design hold at 0.7 — caps of 3.5 units at the cell edges are the first thing I would
  expect to break.
