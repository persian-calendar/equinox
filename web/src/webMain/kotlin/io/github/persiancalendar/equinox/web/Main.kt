@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.persiancalendar.equinox.web

import io.github.persiancalendar.equinox.Equinox
import kotlin.js.ExperimentalWasmJsInterop

external fun equinoxReady(equinoxForYear: (Int) -> Double)

fun main() {
    equinoxReady { year: Int -> (Equinox.NorthwardEquinox of year).toDouble() }
}
