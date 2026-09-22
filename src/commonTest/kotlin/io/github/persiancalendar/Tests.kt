package io.github.persiancalendar

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

private const val MAX_ERROR_MILLIS = 1_000L

// === BEGIN GENERATED IRAN GROUND TRUTH (do not edit) ===
// Regenerate with:  ./gradlew generateSources
// Official Tehran University announced spring-equinox
// (Nowruz) moments, given in IRST (UTC+3:30) and rounded to whole
// seconds. These are NOT derived from DE440, so each entry carries a
// per-year tolerance.
// Entries: Triple(year, epoch_millis_utc, tolerance_seconds).
internal val IRAN_GROUND_TRUTH: List<Triple<Int, Long, Int>> = listOf(
    Triple(2002, 1016651762000, 10),
    Triple(2003, 1048208385000, 5),
    Triple(2004, 1079765317000, 5),
    Triple(2005, 1111322004000, 5),
    Triple(2006, 1142879135000, 5),
    Triple(2007, 1174435646000, 5),
    Triple(2008, 1205992099000, 5),
    Triple(2009, 1237549419000, 5),
    Triple(2010, 1269106333000, 5),
    Triple(2011, 1300663245000, 5),
    Triple(2012, 1332220467000, 5),
    Triple(2013, 1363777316000, 5),
    Triple(2014, 1395334627000, 5),
    Triple(2015, 1426891511000, 5),
    Triple(2016, 1458448212000, 5),
    Triple(2017, 1490005720000, 6),
    Triple(2018, 1521562528000, 5),
    Triple(2019, 1553119107000, 5),
    Triple(2020, 1584676177000, 5),
    Triple(2021, 1616233048000, 5),
    Triple(2022, 1647790406000, 5),
    Triple(2023, 1679347468000, 5),
    Triple(2024, 1710903986000, 5),
    Triple(2025, 1742461290000, 5),
    Triple(2026, 1774017959000, 5),
)
// === END GENERATED IRAN GROUND TRUTH ===

class Tests {

    @Test
    fun allSeasonsMatchDe440WithinOneSecond() {
        for (row in DE440_REFERENCE) {
            val year = row[0].toInt()
            for (season in 0..3) {
                val actual = Equinox.entries[season] of year
                val expected = row[season + 1]
                val delta = abs(actual - expected)
                assertTrue(
                    delta < MAX_ERROR_MILLIS,
                    "year=$year season=$season: off by ${delta}ms (expected $expected, got $actual)"
                )
            }
        }
    }

    @Test
    fun marchEquinoxSpotCheck() {
        for ((year, expected) in MARCH_SPOT_CHECK) {
            val actual = Equinox.NORTHWARD_EQUINOX of year
            val delta = abs(actual - expected)
            assertTrue(
                delta < MAX_ERROR_MILLIS,
                "year=$year March equinox: off by ${delta}ms (expected $expected, got $actual)"
            )
        }
    }

    @Test
    fun allSeasonsSpotCheck() {
        for ((year, season, expected) in ALL_SEASONS_SPOT_CHECK) {
            val actual = Equinox.entries[season] of year
            val delta = abs(actual - expected)
            assertTrue(
                delta < MAX_ERROR_MILLIS,
                "year=$year season=$season: off by ${delta}ms (expected $expected, got $actual)"
            )
        }
    }

    @Test
    fun doesNotThrowAcrossExtendedRange() {
        for (year in -2000..10000) {
            Equinox.NORTHWARD_EQUINOX of year
        }
    }

    @Test
    fun iranGroundTruthWithinTolerance() {
        for ((year, expectedMillis, toleranceSeconds) in IRAN_GROUND_TRUTH) {
            val actual = Equinox.NORTHWARD_EQUINOX of year
            val delta = abs(actual - expectedMillis)
            println(
                "Nowruz declared by Tehran University for $year A.D.: off by ${delta}ms " +
                    "(tolerance=${toleranceSeconds}s; expected $expectedMillis, got $actual)"
            )
            assertTrue(
                delta <= toleranceSeconds * 1000L,
                "year=$year: off by ${delta}ms " +
                    "(tolerance=${toleranceSeconds}s; expected $expectedMillis, got $actual)"
            )
        }
    }
}
