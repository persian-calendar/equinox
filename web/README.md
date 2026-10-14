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

The page is plain HTML, CSS and Kotlin; the only asset is the digits font. When
the icons are regenerated, `rsvg-convert` (from `librsvg`) has to be installed:

```sh
./gradlew :web:generateWebIcons
```
