package io.github.persiancalendar

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

private const val MAX_ERROR_MILLIS = 1_000L

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
}
