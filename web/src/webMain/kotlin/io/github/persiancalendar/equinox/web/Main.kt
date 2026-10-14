@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.persiancalendar.equinox.web

import io.github.persiancalendar.equinox.Equinox
import io.github.persiancalendar.equinox.web.dom.Date
import io.github.persiancalendar.equinox.web.dom.DomElement
import io.github.persiancalendar.equinox.web.dom.document
import io.github.persiancalendar.equinox.web.dom.setInterval
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.math.roundToInt

private const val SECOND = 1000L
private const val MINUTE = 60 * SECOND
private const val HOUR = 60 * MINUTE
private const val DAY = 24 * HOUR

/** Iran dropped DST in 2022, so every upcoming equinox is at a fixed UTC+03:30. */
private const val TEHRAN_OFFSET = (3.5 * 3600 * 1000).toLong()

/** Nowruz of the Gregorian year Y falls in the Persian year Y - 621. */
private const val NOWRUZ_YEAR_OFFSET = 621

/** The countdown is recomputed this often; a card only fades when its digit changes. */
private const val TICK_MILLIS = 100

/** The clock turns gold during the last ten seconds. */
private const val IMMINENT_MILLIS = 10 * SECOND

/** How long the «نوروز مبارک» banner stays up once the equinox has passed. */
private const val CELEBRATION_MILLIS = 20 * SECOND

private const val CHANGING_CLASS = "is-changing"

private val monthNames = listOf(
    "ژانویه", "فوریه", "مارس", "آوریل", "مه", "ژوئن",
    "ژوئیه", "اوت", "سپتامبر", "اکتبر", "نوامبر", "دسامبر",
)

private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

/** Rewrites Latin digits of [text] as Persian ones. */
private fun persian(text: String): String = buildString(text.length) {
    text.forEach { append(if (it in '0'..'9') persianDigits[it - '0'] else it) }
}

private fun persian(number: Long): String = persian(number.toString())

private fun padded(number: Int): String = persian(number.toString().padStart(2, '0'))

private fun millisNow(): Long = Date().getTime().toLong()

private fun element(id: String): DomElement =
    document.getElementById<DomElement>(id) ?: error("index.html has no #$id")

private fun number(value: Double): String = ((value * 1000).roundToInt() / 1000.0).toString()

/** The next March equinox after [time], the one before it, and the Nowruz year it starts. */
private class Target(val equinox: Long, val previousEquinox: Long, val nowruzYear: Int) {
    val span: Long get() = equinox - previousEquinox
}

private val equinoxCache = HashMap<Int, Long>()

private fun marchEquinox(year: Int): Long =
    equinoxCache.getOrPut(year) { Equinox.NorthwardEquinox of year }

private fun targetAt(time: Long): Target {
    val year = Date(time.toDouble()).getUTCFullYear()
    val march = marchEquinox(year)
    return if (time < march) {
        Target(march, marchEquinox(year - 1), year - NOWRUZ_YEAR_OFFSET)
    } else {
        Target(marchEquinox(year + 1), march, year + 1 - NOWRUZ_YEAR_OFFSET)
    }
}

private fun formatInstant(equinox: Long): String {
    val tehran = Date((equinox + TEHRAN_OFFSET).toDouble())
    return persian(tehran.getUTCDate().toLong()) +
        " " + monthNames[tehran.getUTCMonth()] +
        " " + persian(tehran.getUTCFullYear().toLong()) +
        "، ساعت " + padded(tehran.getUTCHours()) +
        ":" + padded(tehran.getUTCMinutes()) +
        ":" + padded(tehran.getUTCSeconds()) +
        " به وقت تهران"
}

/** Restarts the crossfade of [card] by toggling the state class. */
@Suppress("UNUSED_EXPRESSION")
private fun restartCrossfade(card: DomElement) {
    card.classList.remove(CHANGING_CLASS)
    // Reading a layout property flushes the removal, so that adding the class
    // back is seen as a fresh animation instead of no change at all.
    card.offsetHeight
    card.classList.add(CHANGING_CLASS)
}

/** One card: the digit that is fading out on top of the digit that replaces it. */
private class Digit(private val id: String) {
    private val card = element(id)
    private val previousDigit = layer(".digit--previous")
    private val currentDigit = layer(".digit--current")

    private var value = -1

    private fun layer(selector: String): DomElement =
        card.querySelector<DomElement>(selector) ?: error("#$id has no $selector")

    private fun glyph(layer: DomElement, digit: Int) {
        layer.setAttribute("data-v", persian(digit.toLong()))
    }

    fun show(digit: Int, animate: Boolean) {
        if (digit == value) return
        val previous = value
        value = digit

        glyph(currentDigit, digit)

        if (previous < 0 || !animate) {
            glyph(previousDigit, digit)
            return
        }

        glyph(previousDigit, previous)
        restartCrossfade(card)
    }
}

/** A group of cards showing one zero-padded number, e.g. `007` days. */
private class Digits(ids: List<String>) {
    private val cards = ids.map { Digit(it) }

    fun show(number: Int, animate: Boolean) {
        var rest = number
        for (index in cards.indices.reversed()) {
            cards[index].show(rest % 10, animate)
            rest /= 10
        }
    }
}

fun main() {
    val app = element("app")
    val clock = element("clock")
    val yearLabel = element("year")
    val instantLabel = element("instant")
    val progressNote = element("progress-note")

    val days = Digits(listOf("days-2", "days-1", "days-0"))
    val hours = Digits(listOf("hours-1", "hours-0"))
    val minutes = Digits(listOf("minutes-1", "minutes-0"))
    val seconds = Digits(listOf("seconds-1", "seconds-0"))

    var target = targetAt(millisNow())
    var celebratingUntil = 0L
    var lastLabel = ""
    var painted = false

    fun renderTarget() {
        yearLabel.textContent = persian(target.nowruzYear.toLong())
        instantLabel.textContent = formatInstant(target.equinox)
        progressNote.textContent = "از نوروز " + persian((target.nowruzYear - 1).toLong()) +
            " تا نوروز " + persian(target.nowruzYear.toLong())
    }

    fun tick() {
        val time = millisNow()
        if (time >= target.equinox) {
            celebratingUntil = time + CELEBRATION_MILLIS
            target = targetAt(time)
            renderTarget()
        }

        val remaining = (target.equinox - time).coerceAtLeast(0L)
        val dayCount = (remaining / DAY).toInt()
        val hourCount = ((remaining / HOUR) % 24).toInt()
        val minuteCount = ((remaining / MINUTE) % 60).toInt()
        val secondCount = ((remaining / SECOND) % 60).toInt()

        days.show(dayCount, painted)
        hours.show(hourCount, painted)
        minutes.show(minuteCount, painted)
        seconds.show(secondCount, painted)
        painted = true

        val label = persian(dayCount.toLong()) + " روز و " + persian(hourCount.toLong()) +
            " ساعت و " + persian(minuteCount.toLong()) + " دقیقه و " +
            persian(secondCount.toLong()) + " ثانیه تا نوروز"
        if (label != lastLabel) {
            lastLabel = label
            clock.setAttribute("aria-label", label)
        }

        app.style.setProperty(
            "--progress",
            number((time - target.previousEquinox).toDouble() / target.span),
        )
        app.style.setProperty(
            "--glow",
            number(1.0 - (remaining.toDouble() / DAY).coerceIn(0.0, 1.0)),
        )

        if (remaining <= IMMINENT_MILLIS) app.classList.add("is-imminent")
        else app.classList.remove("is-imminent")

        if (time < celebratingUntil) app.classList.add("is-nowruz")
        else app.classList.remove("is-nowruz")
    }

    renderTarget()
    tick()
    setInterval({ tick() }, TICK_MILLIS)
}
