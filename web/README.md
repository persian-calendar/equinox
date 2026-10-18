# Equinox Web

A tiny HTML demo of this library: a countdown to the next March equinox, i.e. to
Nowruz. The remaining days, hours, minutes and seconds are computed with
`Equinox.NorthwardEquinox of year` in the browser.

https://persian-calendar.github.io/equinox/

## Run locally

From the repository root:

```sh
# JS backend (faster to build)
./gradlew :web:jsBrowserDevelopmentRun

# Wasm backend
./gradlew :web:wasmJsBrowserDevelopmentRun
```

Then open http://localhost:8080/.

## Build a static bundle

```sh
# JS bundle  -> web/build/dist/js/productionExecutable/
./gradlew :web:jsBrowserDistribution

# Wasm bundle -> web/build/dist/wasmJs/productionExecutable/
./gradlew :web:wasmJsBrowserDistribution
```

Either bundle directory is complete and can be served as it is. The page's own logic is
`main.js` and the Kotlin is compiled to `web.js`; the only other asset is the digits font.
When the icons are regenerated, `rsvg-convert` (from `librsvg`) has to be installed:

```sh
./gradlew :web:generateWebIcons
```

## A single file build

`webStandalone` copies the JS distribution to `web/build/dist/js/standalone/` with `main.js`
and `web.js` written into `index.html` instead of being loaded as separate files, and the
digits font written in as a base64 `data:` URL instead of a file of its own. Publishing that
directory means publishing one document, with no separate requests and no chance of a cached
page running against a newer script.

```sh
./gradlew :web:webStandalone
```

The task leaves the normal distribution alone, so `jsBrowserDistribution` still produces the
separate files for development. Three things are adjusted in the copy: the source map comments
go, since the maps are no longer files of their own; the font file is dropped once its bytes
are in the page, which at four kilobytes costs less than a request and has no cache entry to
go stale; and all three are dropped from the service worker's precache list, which has nothing
left to fetch for them.

Only the JS target is handled. The Wasm build's glue loads a `.wasm` file of its own, and that
has to stay a separate request, while the JS bundle is self-contained.

The service worker is stamped with the commit at deploy time, not by this task: the
browser only looks for a new worker when `sw.js` has changed, so putting the revision inside
it is what turns a deploy into a new cache and the end of the old one. A local build leaves it
at `'dev'`, where the name of the cache does not matter.
