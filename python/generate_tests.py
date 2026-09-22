#!/usr/bin/env python3
"""
Regenerate the two test data sources under src/commonTest/kotlin/.../:

1. De440Reference.kt — JPL DE440 season instants for every supported year
   (1800-2200), as Kotlin source data consumed by the commonTest suite (no
   classpath resources needed, so the tests run on every KMP target).

2. The IRAN_GROUND_TRUTH block inside Tests.kt — the official Tehran University
   announced spring-equinox (Nowruz) moments, read from
   the python/equinox-research git submodule and written between the
   "GENERATED IRAN GROUND TRUTH" markers in Tests.kt. These moments are NOT
   derived from DE440, so each entry is emitted with a per-year tolerance in
   seconds, and a comparison table is printed to stdout.

Requires:  pip install skyfield   and a DE440 ephemeris (de440.bsp) either in
~/.skyfield or referenced via --bsp.
"""

from __future__ import annotations

import argparse
import datetime
import json
import math
import os

from skyfield import almanac
from skyfield.api import load, load_file

SCRIPT_DIR = os.path.dirname(__file__)

BSP_CANDIDATES = [
    os.path.expanduser("~/.skyfield/de440.bsp"),
]

DE440_REF_PATH = os.path.join(
    SCRIPT_DIR, '..',
    'src/commonTest/kotlin/io/github/persiancalendar/De440Reference.kt')

TESTS_PATH = os.path.join(
    SCRIPT_DIR, '..',
    'src/commonTest/kotlin/io/github/persiancalendar/Tests.kt')

GROUND_TRUTH_PATH = os.path.join(SCRIPT_DIR, 'equinox-research', 'iran-ground-truth.json')

START_YEAR = 1800
END_YEAR = 2200  # inclusive

MARCH_SPOT_YEARS = list(range(1800, 2201, 10)) + list(range(2002, 2023))
ALL_SEASONS_SPOT_YEARS = [1800, 1850, 1900, 1950, 2000, 2020, 2050, 2100, 2200]

# Iran Standard Time (UTC+3:30), used by the announced Nowruz moments.
IRST = datetime.timezone(datetime.timedelta(hours=3, minutes=30))

BEGIN_MARKER = '// === BEGIN GENERATED IRAN GROUND TRUTH (do not edit) ==='
END_MARKER = '// === END GENERATED IRAN GROUND TRUTH ==='


def season_millis(ts, eph, year: int, season: int) -> int:
    t0 = ts.utc(year, 1, 1)
    t1 = ts.utc(year + 1, 1, 1)
    times, events = almanac.find_discrete(t0, t1, almanac.seasons(eph))
    for t, e in zip(times, events):
        if e == season:
            return int(round(t.utc_datetime().timestamp() * 1000))
    raise RuntimeError(f"no season {season} in {year}")


def ground_truth_millis(row) -> int:
    """Convert a [year, month, day, hour, minute, second, url] IRST row to UTC epoch millis."""
    year, month, day, hour, minute, second = row[:6]
    dt = datetime.datetime(year, month, day, hour, minute, second, tzinfo=IRST)
    return int(dt.timestamp() * 1000)


def build_ground_truth(ts, eph):
    if not os.path.exists(GROUND_TRUTH_PATH):
        raise SystemExit(
            f"Ground-truth JSON not found at {os.path.relpath(GROUND_TRUTH_PATH)}. "
            "Initialize the submodule with:  git submodule update --init --recursive"
        )
    with open(GROUND_TRUTH_PATH) as f:
        data = json.load(f)

    print("Iran ground truth vs DE440 (official announced spring equinox):")
    print(f"{'year':>5} {'ground_truth_ms':>16} {'de440_ms':>15} {'delta_s':>9} {'tolerance_s':>12}")
    entries = []
    for row in data:
        year = row[0]
        gt_ms = ground_truth_millis(row)
        de440_ms = season_millis(ts, eph, year, 0)
        delta_s = (gt_ms - de440_ms) / 1000.0
        tolerance_s = max(5, math.ceil(abs(delta_s)) + 3)
        print(f"{year:>5} {gt_ms:>16} {de440_ms:>15} {delta_s:>9.1f} {tolerance_s:>12}")
        entries.append((year, gt_ms, tolerance_s))
    entries.sort(key=lambda r: r[0])
    return entries


def emit_ground_truth_block(entries):
    lines = "\n".join(
        f"    Triple({year}, {ms}, {tolerance}),"
        for year, ms, tolerance in entries
    )
    return (
        f"{BEGIN_MARKER}\n"
        "// Regenerate with:  ./gradlew generateSources\n"
        "// Official Tehran University announced spring-equinox\n"
        "// (Nowruz) moments, given in IRST (UTC+3:30) and rounded to whole\n"
        "// seconds. These are NOT derived from DE440, so each entry carries a\n"
        "// per-year tolerance.\n"
        "// Entries: Triple(year, epoch_millis_utc, tolerance_seconds).\n"
        "internal val IRAN_GROUND_TRUTH: List<Triple<Int, Long, Int>> = listOf(\n"
        f"{lines}\n"
        ")\n"
        f"{END_MARKER}"
    )


def update_tests(block: str) -> None:
    with open(TESTS_PATH) as f:
        content = f.read()
    if BEGIN_MARKER in content and END_MARKER in content:
        start = content.index(BEGIN_MARKER)
        end = content.index(END_MARKER) + len(END_MARKER)
        content = content[:start] + block + content[end:]
    else:
        content = content.rstrip() + "\n\n" + block + "\n"
    with open(TESTS_PATH, 'w') as f:
        f.write(content)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--bsp", type=str, default=None, help="path to de440.bsp")
    args = parser.parse_args()

    ts = load.timescale()
    if args.bsp:
        eph = load_file(args.bsp)
    else:
        local = next((p for p in BSP_CANDIDATES if os.path.exists(p)), None)
        eph = load_file(local) if local else load("de440.bsp")

    rows = []
    for year in range(START_YEAR, END_YEAR + 1):
        values = [season_millis(ts, eph, year, s) for s in range(4)]
        rows.append((year, values))

    march_rows = [(y, season_millis(ts, eph, y, 0)) for y in MARCH_SPOT_YEARS]
    seasons_rows = [(y, s, season_millis(ts, eph, y, s))
                    for y in ALL_SEASONS_SPOT_YEARS for s in range(4)]

    full = "\n".join(
        f"    longArrayOf({', '.join(str(v) for v in [year] + values)}),"
        for year, values in rows
    )
    march = "\n".join(f"    {y} to {m}," for y, m in march_rows)
    seasons = "\n".join(f"    Triple({y}, {s}, {m})," for y, s, m in seasons_rows)

    kotlin = f'''package io.github.persiancalendar

// GENERATED FILE — DO NOT EDIT BY HAND.
// Regenerate with:  ./gradlew generateSources
// (or:  python3 python/generate_tests.py)
//
// JPL DE440 season instants for the supported years {START_YEAR}-{END_YEAR}.
// Each full row is (year, northward_equinox, northern_solstice,
// southward_equinox, southern_solstice) in epoch milliseconds (UTC).

internal val DE440_REFERENCE: List<LongArray> = listOf(
{full}
)

internal val MARCH_SPOT_CHECK: List<Pair<Int, Long>> = listOf(
{march}
)

internal val ALL_SEASONS_SPOT_CHECK: List<Triple<Int, Int, Long>> = listOf(
{seasons}
)
'''

    os.makedirs(os.path.dirname(DE440_REF_PATH), exist_ok=True)
    with open(DE440_REF_PATH, 'w') as f:
        f.write(kotlin)
    print("wrote", os.path.abspath(DE440_REF_PATH))

    entries = build_ground_truth(ts, eph)
    update_tests(emit_ground_truth_block(entries))
    print("updated", os.path.abspath(TESTS_PATH))


if __name__ == "__main__":
    main()
