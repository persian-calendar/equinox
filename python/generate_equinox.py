#!/usr/bin/env python3
"""
Generate Equinox.kt: a dependency-free, high-accuracy equinox/solstice
algorithm based on VSOP87 (Earth), IAU2000B nutation and aberration,
calibrated against JPL DE440.

Usage:  python3 python/generate_equinox.py
"""

from __future__ import annotations

import math
import os

import numpy as np
from skyfield import almanac
from skyfield.api import load, load_file
from skyfield.functions import load_bundled_npy

ASEC2RAD = math.pi / (180.0 * 3600.0)
TENTH_USEC_2_RAD = ASEC2RAD / 1e7
L_THRESH = 3e-9
R_THRESH = 1e-7
ABERRATION = 20.4898

OUT_PATH = os.path.join(os.path.dirname(__file__), '..',
                        'src/commonMain/kotlin/io/github/persiancalendar/Equinox.kt')


def parse_vsop87d(path):
    series = {1: {}, 3: {}}
    var = power = None
    with open(path) as f:
        for line in f:
            toks = line.split()
            if not toks:
                continue
            if toks[0] == 'VSOP87':
                var = int(toks[5])
                power = int(toks[7][4:])
                if var in series and power not in series[var]:
                    series[var][power] = []
                continue
            if toks[0] == '43' + str(var) + str(power) and var in series:
                series[var][power].append((float(toks[-3]), float(toks[-2]), float(toks[-1])))
    return series


def truncate(series):
    out = {1: {}, 3: {}}
    for var, thr in ((1, L_THRESH), (3, R_THRESH)):
        for p, terms in series[var].items():
            out[var][p] = [(A, B, C) for A, B, C in terms if abs(A) >= thr]
    return out


_npz = load_bundled_npy('nutation.npz')
NALS = _npz['nals_t'][:77]
NUT_LONG = _npz['lunisolar_longitude_coefficients'][:77]
FA0 = [485868.249036, 1287104.79305, 335779.526232, 1072260.70369, 450160.398036]
FA1 = [1717915923.2178, 129596581.0481, 1739527262.8478, 1602961601.2090, -6962890.5431]


def apparent_longitude_deg(T, series, dpsi):
    L = 0.0
    R = 0.0
    for p in (0, 1, 2, 3, 4, 5):
        sL = sum(A * math.cos(B + C * T) for A, B, C in series[1].get(p, ()))
        sR = sum(A * math.cos(B + C * T) for A, B, C in series[3].get(p, ()))
        L += sL * T ** p
        R += sR * T ** p
    lam = math.degrees(L + math.pi) % 360.0
    lam -= ABERRATION / R / 3600.0
    lam += math.degrees(dpsi)
    return lam % 360.0


def own_dpsi(jd_tt):
    t = (jd_tt - 2451545.0) / 36525.0
    fa = [f0 + f1 * t for f0, f1 in zip(FA0, FA1)]
    dpsi = 0.0
    for i in range(77):
        arg = sum(NALS[i][j] * fa[j] for j in range(5)) * ASEC2RAD
        s = math.sin(arg)
        c = math.cos(arg)
        dpsi += NUT_LONG[i][0] * s + NUT_LONG[i][1] * s * t + NUT_LONG[i][2] * c
    dpsi += -0.000135e7
    return dpsi * TENTH_USEC_2_RAD


def gregorian_to_jd(year, month, day):
    a = (14 - month) // 12
    y = year + 4800 - a
    m = month + 12 * a - 3
    jdn = day + (153 * m + 2) // 5 + 365 * y + y // 4 - y // 100 + y // 400 - 32045
    return float(jdn)  # noon


MEAN_MOTION_DEG_DAY = 360.0 / 365.2422


def find_event_newton(series, year, i):
    target_deg = [0.0, 90.0, 180.0, 270.0][i]
    m, d = [(3, 20), (6, 21), (9, 22), (12, 21)][i]
    t = gregorian_to_jd(year, m, d)
    for _ in range(10):
        T = (t - 2451545.0) / 365250.0
        lam = apparent_longitude_deg(T, series, own_dpsi(t))
        diff = (lam - target_deg + 180.0) % 360.0 - 180.0
        t -= diff / MEAN_MOTION_DEG_DAY
    return t


def fmt(v):
    """Format a number as a Kotlin Double literal (always keeps a decimal)."""
    s = "%.12g" % v
    if 'e' not in s and '.' not in s:
        s += ".0"
    return s


def emit_nested(values, per_line=3):
    """Emit listOf(...) lines, each item already a full expression string."""
    lines = []
    for i in range(0, len(values), per_line):
        lines.append("            " + ", ".join(values[i:i + per_line]) + ",")
    return "\n".join(lines)


def emit_flat(values, per_line=8):
    """Emit a flat array literal of Kotlin Double values."""
    lines = []
    cur = []
    for v in values:
        cur.append(fmt(v))
        if len(cur) == per_line:
            lines.append("            " + ", ".join(cur) + ",")
            cur = []
    if cur:
        lines.append("            " + ", ".join(cur) + ",")
    return "\n".join(lines)


def main():
    bsp = os.path.expanduser("~/.skyfield/de440.bsp")
    vsop_path = os.path.expanduser("~/.skyfield/VSOP87D.ear")
    ts = load.timescale()
    eph = load_file(bsp)
    series = truncate(parse_vsop87d(vsop_path))
    nL = sum(len(v) for v in series[1].values())
    nR = sum(len(v) for v in series[3].values())

    # validate + fit correction
    rows = []
    for year in range(1550, 2650):
        t0 = ts.utc(year, 1, 1)
        t1 = ts.utc(year + 1, 1, 1)
        times, ev = almanac.find_discrete(t0, t1, almanac.seasons(eph))
        for i in range(4):
            ref = next(t.tt for t, e in zip(times, ev) if e == i)
            my = find_event_newton(series, year, i)
            rows.append((year, i, my, (my - ref) * 86400.0))
    yr = np.array([r[0] for r in rows])
    my_jd = np.array([r[2] for r in rows])
    err = np.array([r[3] for r in rows])
    T = (my_jd - 2451545.0) / 36525.0
    X = np.column_stack([np.ones_like(T), T, T ** 2])
    coef, *_ = np.linalg.lstsq(X, err, rcond=None)
    # Round to a fixed precision so the emitted constants are bit-for-bit
    # identical across platforms: np.linalg.lstsq delegates to LAPACK, whose
    # SVD differs slightly between OS BLAS implementations (e.g. macOS
    # Accelerate vs Linux OpenBLAS), which would otherwise shift the fit in
    # the 7th decimal and break the `checkGeneratedSources` CI gate.
    coef = np.round(coef, 5)
    resid = err - X @ coef
    print(f"L={nL} R={nR} terms")
    print(f"correction c0={coef[0]:+.5f} c1={coef[1]:+.5f} c2={coef[2]:+.5f}")
    for lo, hi in [(1800, 2200), (1900, 2100), (2002, 2022)]:
        mask = (yr >= lo) & (yr <= hi)
        print(f"  {lo}-{hi}: std={resid[mask].std():.3f}s max|err|={np.abs(resid[mask]).max():.3f}s")

    # flatten terms
    L_flat = []
    for p in (0, 1, 2, 3, 4, 5):
        for A, B, C in series[1].get(p, ()):
            L_flat.append((A, B, C, p))
    R_flat = []
    for p in (0, 1, 2, 3, 4, 5):
        for A, B, C in series[3].get(p, ()):
            R_flat.append((A, B, C, p))

    l_vals = []
    for A, B, C, p in L_flat:
        l_vals += [A, B, C, float(p)]
    r_vals = []
    for A, B, C, p in R_flat:
        r_vals += [A, B, C, float(p)]

    # Precompute each term's phase (radians) and frequency (radians/century) so
    # the runtime only needs `angle = phase + frequency * t`.
    nut_vals = []
    for i in range(77):
        phase = sum(NALS[i][j] * FA0[j] for j in range(5)) * ASEC2RAD
        freq = sum(NALS[i][j] * FA1[j] for j in range(5)) * ASEC2RAD
        c0, c1, c2 = NUT_LONG[i]
        nut_vals += [phase, freq, float(c0), float(c1), float(c2)]

    leap_rows = [
        (1972, 1, 10), (1972, 7, 11), (1973, 1, 12), (1974, 1, 13),
        (1975, 1, 14), (1976, 1, 15), (1977, 1, 16), (1978, 1, 17),
        (1979, 1, 18), (1980, 1, 19), (1981, 7, 20), (1982, 7, 21),
        (1983, 7, 22), (1985, 7, 23), (1988, 1, 24), (1990, 1, 25),
        (1991, 1, 26), (1992, 7, 27), (1993, 7, 28), (1994, 7, 29),
        (1996, 1, 30), (1997, 7, 31), (1999, 1, 32), (2006, 1, 33),
        (2009, 1, 34), (2012, 7, 35), (2015, 7, 36), (2017, 1, 37),
    ]
    leap_items = [f"Triple({y}, {m}, {o})" for y, m, o in reversed(leap_rows)]

    kotlin = f'''package io.github.persiancalendar

// GENERATED FILE — DO NOT EDIT BY HAND.
// Regenerate with:  ./gradlew generateSources
// (or:  python3 python/generate_equinox.py)

// High-accuracy equinox and solstice computation, calibrated against the
// JPL DE440 ephemeris. The Sun's apparent geocentric ecliptic longitude is
// computed from the VSOP87 Earth theory (truncated), IAU2000B nutation and
// annual aberration, then the instant at which it reaches 0/90/180/270 degrees
// is located with Newton's method and converted to Unix/POSIX epoch
// milliseconds using the TAI-UTC leap-second table.
//
// Accuracy vs DE440: within about 1 second for years 1800-2200 (all seasons).
//
// Data sources:
//   - VSOP87D.ear (Bretagnon & Francou, IMCCE): Earth heliocentric series
//   - IAU2000B nutation coefficients (IERS Conventions)
//   - JPL DE440 ephemeris (Park et al. 2021), used only to calibrate a small
//     empirical correction term (see generate_equinox.py).

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin

enum class Equinox(
    private val target: Double,
    private val month: Int,
    private val day: Int,
) {{
    /** Spring equinox for the northern hemisphere (March). */
    NORTHWARD_EQUINOX(0.0, 3, 20),
    /** Summer solstice for the northern hemisphere (June). */
    NORTHERN_SOLSTICE(90.0, 6, 21),
    /** Fall equinox for the northern hemisphere (September). */
    SOUTHWARD_EQUINOX(180.0, 9, 22),
    /** Winter solstice for the northern hemisphere (December). */
    SOUTHERN_SOLSTICE(270.0, 12, 21);

    /** The instant of this season in the given Gregorian year, as Unix/POSIX epoch milliseconds. */
    infix fun of(year: Int): Long {{
        val jdTT = findEvent(year, target, month, day)
        val t = (jdTT - J2000) / 36525.0
        val correction = CORRECTION_C0 + t * (CORRECTION_C1 + t * CORRECTION_C2)
        val jdPosix = jdTT - (correction + 32.184 + taiMinusUtc(year, month)) / 86400.0
        return ((jdPosix - UNIX_EPOCH_JD) * 86400000.0).roundToLong()
    }}

    private companion object {{
        private const val J2000 = 2451545.0
        private const val UNIX_EPOCH_JD = 2440587.5
        private const val ASEC2RAD = PI / (180.0 * 3600.0)
        private const val ABERRATION = 20.4898  // solar aberration, arcsec
        private const val MEAN_MOTION = 360.0 / 365.2422  // deg/day
        private const val CORRECTION_C0 = {coef[0]:+.5f}
        private const val CORRECTION_C1 = {coef[1]:+.5f}
        private const val CORRECTION_C2 = {coef[2]:+.5f}

        // VSOP87 Earth terms: flat groups of (A, B, C, power); each group
        // contributes A * T^power * cos(B + C*T), where T is in Julian millennia
        // since J2000. Generated from VSOP87D.ear, truncated to |A| >= {L_THRESH:g} rad.
        private val EARTH_L = doubleArrayOf(
{emit_flat(l_vals)}
        )

        // VSOP87 Earth radius terms (same layout), truncated to |A| >= {R_THRESH:g} AU.
        private val EARTH_R = doubleArrayOf(
{emit_flat(r_vals)}
        )

        // IAU2000B luni-solar nutation, flat groups of (phase, frequency, c0, c1, c2):
        // angle = phase + frequency * t, and each term contributes
        // c0*sin(angle) + c1*sin(angle)*t + c2*cos(angle) (tenths of micro-arcsec).
        private val NUT_TERMS = doubleArrayOf(
{emit_flat(nut_vals)}
        )

        // (year, month, TAI-UTC) leap-second offsets, newest first.
        private val LEAP_SECONDS = listOf(
{emit_nested(leap_items)}
        )

        private fun earthL(T: Double): Double =
            (EARTH_L.indices step 4).sumOf {{ i ->
                EARTH_L[i] * T.pow(EARTH_L[i + 3]) * cos(EARTH_L[i + 1] + EARTH_L[i + 2] * T)
            }}

        private fun earthR(T: Double): Double =
            (EARTH_R.indices step 4).sumOf {{ i ->
                EARTH_R[i] * T.pow(EARTH_R[i + 3]) * cos(EARTH_R[i + 1] + EARTH_R[i + 2] * T)
            }}

        /** IAU2000B nutation in longitude (radians) at the given TT Julian date. */
        private fun nutationLongitude(jdTT: Double): Double {{
            val t = (jdTT - J2000) / 36525.0
            val sum = (NUT_TERMS.indices step 5).sumOf {{ i ->
                val angle = NUT_TERMS[i] + NUT_TERMS[i + 1] * t
                val s = sin(angle)
                NUT_TERMS[i + 2] * s + NUT_TERMS[i + 3] * s * t + NUT_TERMS[i + 4] * cos(angle)
            }}
            return (sum - 0.000135e7) * ASEC2RAD / 1e7
        }}

        /** Sun's apparent geocentric ecliptic longitude, true equinox of date (degrees). */
        private fun apparentLongitude(jdTT: Double): Double {{
            val T = (jdTT - J2000) / 365250.0
            val L = earthL(T)
            val R = earthR(T)
            var lon = (L + PI) * 180.0 / PI
            lon -= ABERRATION / R / 3600.0
            lon += nutationLongitude(jdTT) * 180.0 / PI
            return ((lon % 360.0) + 360.0) % 360.0
        }}

        /**
         * TAI - UTC (leap seconds) in effect at the given date. Before 1972 the
         * offset is the fixed 10 seconds; afterwards it follows the IERS leap
         * second announcements. This matches skyfield's UTC timescale, so the
         * returned Date (Unix/POSIX milliseconds) lines up with DE440.
         */
        private fun taiMinusUtc(year: Int, month: Int): Int =
            LEAP_SECONDS.firstOrNull {{ (y, m, _) ->
                year > y || (year == y && month >= m)
            }}?.third ?: 10

        private fun findEvent(year: Int, targetDeg: Double, month: Int, day: Int): Double {{
            var t = gregorianToJd(year, month, day)
            repeat(10) {{
                val longitude = apparentLongitude(t)
                val diff = ((longitude - targetDeg + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
                t -= diff / MEAN_MOTION
            }}
            return t
        }}

        private fun gregorianToJd(year: Int, month: Int, day: Int): Double {{
            val a = (14 - month) / 12
            val y = year + 4800 - a
            val m = month + 12 * a - 3
            val jdn = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
            return jdn.toDouble()  // noon on that date
        }}
    }}
}}
'''

    os.makedirs(os.path.dirname(OUT_PATH), exist_ok=True)
    with open(OUT_PATH, 'w') as f:
        f.write(kotlin)
    print("wrote", os.path.abspath(OUT_PATH))


if __name__ == "__main__":
    main()
