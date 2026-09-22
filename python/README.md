# Python tooling

These scripts regenerate the generated Kotlin artifacts in this project. They
need Python 3.9+, [`skyfield`](https://rhodesmill.org/skyfield/) and a JPL
DE440 ephemeris (`de440.bsp`), which skyfield downloads to `~/.skyfield` on
demand or which can be pointed at with `--bsp`.

Run them together through Gradle, or individually:

```sh
./gradlew generateSources        # runs both scripts below
python3 python/generate_equinox.py
python3 python/generate_tests.py
```

`generate_tests.py` also reads the `python/equinox-research` git submodule, so
initialize it first:

```sh
git submodule update --init --recursive
```

## `generate_equinox.py`

Regenerates `src/commonMain/kotlin/io/github/persiancalendar/Equinox.kt`.

It builds a self-contained, dependency-free equinox/solstice algorithm:

1. **VSOP87 Earth theory** (`VSOP87D.ear`, Bretagnon & Francou, IMCCE) for the
   Sun's heliocentric longitude and distance, truncated to the terms that
   matter (L ≥ 3e-9 rad, R ≥ 1e-7 AU).
2. **IAU 2000B nutation** (77 luni-solar terms) to rotate from the mean to the
   true equinox of date.
3. **Annual aberration** (`-20.4898"/R`).
4. Newton's method to locate the instant the apparent longitude reaches
   0°/90°/180°/270°, and the TAI−UTC leap-second table to convert to a
   Unix/POSIX timestamp.

A small quadratic correction in time (`c0 + c1*T + c2*T²`) closes the residual
VSOP87-vs-DE440 gap; it is fitted by this script against DE440 over 1550–2649.

Accuracy vs DE440 (all four seasons):

| range     | std     | max |
|-----------|---------|-----|
| 1800–2200 | 0.29 s  | 0.93 s |
| 1900–2100 | 0.23 s  | 0.70 s |
| 2002–2022 | 0.22 s  | 0.66 s |

## `generate_tests.py`

Regenerates two test data sources:

1. `src/commonTest/kotlin/io/github/persiancalendar/De440Reference.kt` — the
   JPL DE440 season instants consumed by the `commonTest` suite. It emits three
   Kotlin data structures (the full 1800–2200 table plus two spot-check lists),
   so the tests need no classpath resources and run on every Kotlin
   Multiplatform target.

2. The `IRAN_GROUND_TRUTH` block in
   `src/commonTest/kotlin/io/github/persiancalendar/Tests.kt` — the official
   University of Tehran announced spring-equinox (Nowruz) moments,
   read from `python/equinox-research/iran-ground-truth.json`. These are **not**
   derived from DE440, so the script compares them against DE440 and emits a
   per-year tolerance (seconds). The per-year comparison table is printed to
   stdout, e.g.:

   ```
    year  ground_truth_ms        de440_ms   delta_s  tolerance_s
    2026    1774017959000   1774017957447       1.6            5
    ...
    2002    1016651762000   1016651768344      -6.3           10
   ```
