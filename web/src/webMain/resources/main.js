/*
 * The countdown itself. The only thing this page asks of the Kotlin bundle is
 * the instant of an equinox, which arrives as the argument to equinoxReady()
 * below; everything else - the digits, the cascade, the formatting, the DOM -
 * is here, so the Kotlin side stays a library demo with no DOM knowledge.
 */
(function () {
	'use strict';

	var SECOND = 1000, MINUTE = 60 * SECOND, HOUR = 60 * MINUTE, DAY = 24 * HOUR;
	/* Iran dropped DST in 2022, so every upcoming equinox is at a fixed UTC+03:30. */
	var TEHRAN_OFFSET = 3.5 * 3600 * 1000;
	/* Nowruz of the Gregorian year Y falls in the Persian year Y - 621. */
	var NOWRUZ_YEAR_OFFSET = 621;
	/* The countdown is recomputed this often; a card only rolls when its digit changes. */
	var TICK_MILLIS = 100;
	/* The clock turns gold during the last ten seconds. */
	var IMMINENT_MILLIS = 10 * SECOND;
	/* How long the «نوروز مبارک» banner stays up once the equinox has passed. */
	var CELEBRATION_MILLIS = 20 * SECOND;
	/* The whole cascade budget, shared between the columns that change together. */
	var CASCADE_MILLIS = 150;
	var CHANGING_CLASS = 'is-changing';

	function padded(number) {
		return number.toLocaleString('fa').padStart(2, '0');
	}

	/* Trims a fraction to three decimals, like the progress values the CSS uses. */
	function number(value) {
		return String(Math.round(value * 1000) / 1000);
	}

	function element(id) {
		var found = document.getElementById(id);
		if (!found) throw new Error('index.html has no #' + id);
		return found;
	}

	/* One card: the digit that rolls out while the digit that replaces it rolls in. */
	function createDigit(card) {
		var previousLayer = card.querySelector('.digit--previous');
		var currentLayer = card.querySelector('.digit--current');
		var value = -1;

		function glyph(layer, digit) {
			layer.setAttribute('data-v', digit.toLocaleString('fa'));
		}

		function show(digit, delayMillis) {
			if (digit === value) return;
			var previous = value;
			value = digit;

			glyph(currentLayer, digit);

			/* The first paint shows the digit instead of rolling into it. */
			if (previous < 0) {
				glyph(previousLayer, digit);
				return;
			}

			glyph(previousLayer, previous);
			card.style.setProperty('--roll-delay', delayMillis + 'ms');
			card.classList.remove(CHANGING_CLASS);
			/* Reading a layout property flushes the removal, so that adding the class
			   back is seen as a fresh animation instead of no change at all. */
			void card.offsetHeight;
			card.classList.add(CHANGING_CLASS);
		}

		function getValue() {
			return value;
		}

		return { show: show, getValue: getValue };
	}

	/* A group of cards showing one zero-padded number, e.g. 007 days. */
	function createDigits(ids) {
		var cards = ids.map(function (id) {
			return createDigit(element(id));
		});

		function digitsOf(number) {
			var out = [];
			var rest = number;
			for (var index = cards.length - 1; index >= 0; index--) {
				out[index] = rest % 10;
				rest = Math.floor(rest / 10);
			}
			return out;
		}

		function show(number) {
			var digits = digitsOf(number);
			/* The columns start left to right, most significant first, dividing one
			   shared budget between the columns that actually change. The first paint
			   rolls nothing at all, because no card has a value to replace yet. */
			var changing = digits
				.map(function (digit, index) {
					return digit === cards[index].getValue() ? -1 : index;
				})
				.filter(function (index) {
					return index >= 0;
				});
			var step = changing.length > 1 ? CASCADE_MILLIS / (changing.length - 1) : 0;

			digits.forEach(function (digit, index) {
				var order = changing.indexOf(index);
				cards[index].show(digit, order < 0 ? 0 : order * step);
			});
		}

		return { show: show };
	}

	/* The next March equinox after time, the one before it, and the Nowruz year it starts. */
	function targetAt(time, equinoxForYear) {
		var year = new Date(time).getUTCFullYear();
		var march = equinoxForYear(year);
		if (time < march) {
			return {
				equinox: march,
				previousEquinox: equinoxForYear(year - 1),
				nowruzYear: year - NOWRUZ_YEAR_OFFSET,
			};
		}
		return {
			equinox: equinoxForYear(year + 1),
			previousEquinox: march,
			nowruzYear: year + 1 - NOWRUZ_YEAR_OFFSET,
		};
	}

	/* The instant itself. The browser writes the date from its own calendar data, so
	   it comes out the Persian way (۲۹ اسفند ۱۴۰۵) with no table of month names here;
	   the time is written by hand from the same shifted instant, which keeps the page
	   independent of whether the browser's time zone data agrees with Iran having
	   dropped DST. */
	function formatInstant(equinox) {
		var tehran = new Date(equinox + TEHRAN_OFFSET);
		var date = tehran.toLocaleDateString('fa', {
			calendar: 'persian',
			day: 'numeric',
			month: 'long',
			year: 'numeric',
			timeZone: 'UTC',
		});
		var time = padded(tehran.getUTCHours()) + ':' +
			padded(tehran.getUTCMinutes()) + ':' +
			padded(tehran.getUTCSeconds());
		return date + '، ساعت ' + time + ' به وقت تهران';
	}

	function start(equinoxForYear) {
		var app = element('app');
		var clock = element('clock');
		var yearLabel = element('year');
		var instantLabel = element('instant');
		var progressNote = element('progress-note');

		var days = createDigits(['days-2', 'days-1', 'days-0']);
		var hours = createDigits(['hours-1', 'hours-0']);
		var minutes = createDigits(['minutes-1', 'minutes-0']);
		var seconds = createDigits(['seconds-1', 'seconds-0']);

		var target = targetAt(Date.now(), equinoxForYear);
		var celebratingUntil = 0;
		var lastLabel = '';
		var noThousandsSeparator = { useGrouping: false };

		function renderTarget() {
			yearLabel.textContent = target.nowruzYear.toLocaleString('fa', noThousandsSeparator);
			instantLabel.textContent = formatInstant(target.equinox);
			progressNote.textContent =
				'از نوروز ' + (target.nowruzYear - 1).toLocaleString('fa', noThousandsSeparator) +
				' تا نوروز ' + target.nowruzYear.toLocaleString('fa', noThousandsSeparator);
		}

		function tick() {
			var time = Date.now();
			if (time >= target.equinox) {
				celebratingUntil = time + CELEBRATION_MILLIS;
				target = targetAt(time, equinoxForYear);
				renderTarget();
			}

			var remaining = Math.max(target.equinox - time, 0);
			var dayCount = Math.floor(remaining / DAY);
			var hourCount = Math.floor(remaining / HOUR) % 24;
			var minuteCount = Math.floor(remaining / MINUTE) % 60;
			var secondCount = Math.floor(remaining / SECOND) % 60;

			days.show(dayCount);
			hours.show(hourCount);
			minutes.show(minuteCount);
			seconds.show(secondCount);

			var label = dayCount.toLocaleString('fa') + ' روز و ' + hourCount.toLocaleString('fa') +
				' ساعت و ' + minuteCount.toLocaleString('fa') + ' دقیقه و ' +
				secondCount.toLocaleString('fa') + ' ثانیه تا نوروز';
			if (label !== lastLabel) {
				lastLabel = label;
				clock.setAttribute('aria-label', label);
			}

			app.style.setProperty(
				'--progress',
				number((time - target.previousEquinox) / (target.equinox - target.previousEquinox)),
			);
			app.style.setProperty(
				'--glow',
				number(1 - Math.min(Math.max(remaining / DAY, 0), 1)),
			);

			app.classList.toggle('is-imminent', remaining <= IMMINENT_MILLIS);
			app.classList.toggle('is-nowruz', time < celebratingUntil);
		}

		renderTarget();
		tick();
		setInterval(tick, TICK_MILLIS);
	}

	window.equinoxReady = start;
})();
