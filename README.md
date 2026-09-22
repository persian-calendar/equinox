# Equinox

[![](https://jitpack.io/v/persian-calendar/equinox.svg)](https://jitpack.io/#persian-calendar/equinox)

A dependency-free [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
library that computes equinoxes and solstices to **sub-second accuracy** against
the JPL DE440 ephemeris (1800–2200), for the JVM, JavaScript, and native
(Linux, macOS, Windows) targets.

The Sun's apparent geocentric ecliptic longitude is computed from the VSOP87
Earth theory, IAU 2000B nutation and annual aberration; the instant it reaches
0°/90°/180°/270° is located with Newton's method and converted to Unix/POSIX
epoch milliseconds using the TAI−UTC leap-second table.

## Usage

```kotlin
import io.github.persiancalendar.Equinox

val marchEquinox = Equinox.NORTHWARD_EQUINOX of 2026 // Long, epoch millis (UTC)
val juneSolstice = Equinox.NORTHERN_SOLSTICE of 2026
val septEquinox  = Equinox.SOUTHWARD_EQUINOX of 2026
val decSolstice  = Equinox.SOUTHERN_SOLSTICE of 2026
```

`of` returns the season instant as Unix/POSIX epoch milliseconds.

### Dependency

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        // ...
        maven("https://jitpack.io")
    }
}

// build.gradle.kts
dependencies {
    implementation("com.github.persian-calendar:equinox:x.y.z")
}
```

For other build tools, see [jitpack.io](https://jitpack.io/#persian-calendar/equinox).

## Accuracy

Validated against JPL DE440 (all four seasons):

| range     | std     | max |
|-----------|---------|-----|
| 1800–2200 | 0.29 s  | 0.93 s |
| 1900–2100 | 0.23 s  | 0.70 s |
| 2002–2022 | 0.22 s  | 0.66 s |

## Building and testing

```sh
./gradlew jvmTest         # JVM
./gradlew jsNodeTest      # JavaScript (Node)
./gradlew linuxX64Test    # Linux native
./gradlew macosArm64Test  # macOS native (Apple Silicon)
./gradlew mingwX64Test    # Windows native
```

Each native test task only runs on its matching host; the CI matrix in
`.github/workflows/main.yml` runs them on the appropriate runners.

The equinox algorithm and the DE440 reference data are generated from the
scripts in `python/`. The test data additionally depends on the
[`equinox-research`](https://github.com/persian-calendar/equinox-research)
submodule (official Iranian ground-truth equinox moments), so clone with
`--recursive`:

```sh
git clone --recursive https://github.com/persian-calendar/equinox.git
# or, in an existing clone:
git submodule update --init --recursive

./gradlew generateSources
```

To verify the committed generated sources are in sync with the generators (this
is also run by CI):

```sh
./gradlew checkGeneratedSources
```

## License

MIT
