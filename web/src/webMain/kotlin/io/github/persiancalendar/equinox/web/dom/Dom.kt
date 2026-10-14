@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.persiancalendar.equinox.web.dom

import kotlin.js.ExperimentalWasmJsInterop

// Minimal DOM bindings shared by the JS and WasmJS targets so the demo needs no
// third-party dependencies.

external interface DomTokenList {
    fun add(token: String)
    fun remove(token: String)
}

external interface DomStyle {
    fun setProperty(name: String, value: String)
}

external interface DomElement {
    val classList: DomTokenList
    val style: DomStyle

    var textContent: String

    /* Reading a layout property flushes pending style changes. */
    val offsetHeight: Int

    fun setAttribute(name: String, value: String)
    fun <T : DomElement> querySelector(selector: String): T?
}

external interface DomDocument {
    fun <T : DomElement> getElementById(elementId: String): T?
}

external val document: DomDocument

external class Date {
    constructor()
    constructor(time: Double)

    fun getTime(): Double
    fun getUTCFullYear(): Int
    fun getUTCMonth(): Int
    fun getUTCDate(): Int
    fun getUTCHours(): Int
    fun getUTCMinutes(): Int
    fun getUTCSeconds(): Int
}

external fun setInterval(handler: () -> Unit, timeout: Int): Int
