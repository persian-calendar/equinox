package io.github.persiancalendar

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

private const val MAX_ERROR_MILLIS = 1_000L

// === BEGIN GENERATED IRAN GROUND TRUTH (do not edit) ===
// Regenerate with:  ./gradlew generateSources
// Official University of Tehran announced spring-equinox
// (Nowruz) moments, given in IRST (UTC+3:30) and rounded to whole
// seconds. These are NOT derived from DE440, so each entry carries a
// per-year tolerance.
// Entries: Triple(year, epoch_millis_utc, tolerance_seconds).
internal val IRAN_GROUND_TRUTH: List<Triple<Int, Long, Int>> = listOf(
    Triple(1981, 353955811000, 44),
    Triple(1982, 385512959000, 12),
    Triple(1983, 417069533000, 12),
    Triple(1984, 448626271000, 15),
    Triple(1985, 480183236000, 16),
    Triple(1986, 511740176000, 19),
    Triple(1987, 543297128000, 14),
    Triple(1988, 574853936000, 24),
    Triple(1989, 606410909000, 18),
    Triple(1990, 637967966000, 14),
    Triple(1991, 669524524000, 12),
    Triple(1992, 701081291000, 12),
    Triple(1993, 732638450000, 16),
    Triple(1994, 764195293000, 16),
    Triple(1995, 795752075000, 14),
    Triple(1996, 827308990000, 10),
    Triple(1997, 858866086000, 10),
    Triple(1998, 890423671000, 5),
    Triple(1999, 921980748000, 5),
    Triple(2000, 953537714000, 5),
    Triple(2001, 985095040000, 6),
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
        DE440_REFERENCE.forEach { row ->
            val year = row[0].toInt()
            (0..3).forEach { season ->
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
        MARCH_SPOT_CHECK.forEach { (year, expected) ->
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
        ALL_SEASONS_SPOT_CHECK.forEach { (year, season, expected) ->
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
        (-2000..10000).forEach {
            Equinox.NORTHWARD_EQUINOX of it
            Equinox.NORTHERN_SOLSTICE of it
            Equinox.SOUTHWARD_EQUINOX of it
            Equinox.SOUTHERN_SOLSTICE of it
        }
    }

    @Test
    fun iranGroundTruthWithinTolerance() {
        IRAN_GROUND_TRUTH.forEach { (year, expectedMillis, toleranceSeconds) ->
            val actual = Equinox.NORTHWARD_EQUINOX of year
            val delta = abs(actual - expectedMillis)
            println(
                "Nowruz declared by University of Tehran for $year A.D.: off by ${delta}ms " +
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
