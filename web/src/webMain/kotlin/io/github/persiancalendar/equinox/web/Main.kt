@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.persiancalendar.equinox.web

import io.github.persiancalendar.equinox.Equinox
import kotlin.js.ExperimentalWasmJsInterop

external fun equinoxReady(equinoxForYear: (Int) -> Double)
private fun marchEquinox(year: Int): Double = (Equinox.NorthwardEquinox of year).toDouble()

fun main() {
    equinoxReady(::marchEquinox)
}
