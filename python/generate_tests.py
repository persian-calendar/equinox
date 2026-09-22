#!/usr/bin/env python3
"""
Generate src/commonTest/kotlin/io/github/persiancalendar/De440Reference.kt:
the JPL DE440 season instants for every supported year (1800-2200), as Kotlin
source data consumed by the commonTest suite (no classpath resources needed,
so the tests run on every Kotlin Multiplatform target).

Each full row is (year, northward_equinox, northern_solstice, southward_equinox,
southern_solstice) in epoch milliseconds (UTC).

Requires:  pip install skyfield   and a DE440 ephemeris (de440.bsp) either in
~/.skyfield or referenced via --bsp.
"""

from __future__ import annotations

import argparse
import os

from skyfield import almanac
from skyfield.api import load, load_file

BSP_CANDIDATES = [
    os.path.expanduser("~/.skyfield/de440.bsp"),
]

OUT_PATH = os.path.join(os.path.dirname(__file__), '..',
                        'src/commonTest/kotlin/io/github/persiancalendar/De440Reference.kt')

START_YEAR = 1800
END_YEAR = 2200  # inclusive

MARCH_SPOT_YEARS = list(range(1800, 2201, 10)) + list(range(2002, 2023))
ALL_SEASONS_SPOT_YEARS = [1800, 1850, 1900, 1950, 2000, 2020, 2050, 2100, 2200]


def season_millis(ts, eph, year: int, season: int) -> int:
    t0 = ts.utc(year, 1, 1)
    t1 = ts.utc(year + 1, 1, 1)
    times, events = almanac.find_discrete(t0, t1, almanac.seasons(eph))
    for t, e in zip(times, events):
        if e == season:
            return int(round(t.utc_datetime().timestamp() * 1000))
    raise RuntimeError(f"no season {season} in {year}")


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

    os.makedirs(os.path.dirname(OUT_PATH), exist_ok=True)
    with open(OUT_PATH, 'w') as f:
        f.write(kotlin)
    print("wrote", os.path.abspath(OUT_PATH))


if __name__ == "__main__":
    main()
