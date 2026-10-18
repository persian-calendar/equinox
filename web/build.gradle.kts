@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import java.util.Base64

plugins {
    kotlin("multiplatform") version "2.4.21"
}

kotlin {
    js {
        browser()
        binaries.executable()
    }

    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            // The library is the root project of this build.
            implementation(project(":"))
        }
    }
}

val webIconsSvg = file("src/webMain/resources/icons/icon.svg")
val webIconsDir = file("src/webMain/resources/icons")

val generateWebIcons = tasks.register("generateWebIcons") {
    description = "Generate PNG app icons from icon.svg using rsvg-convert"
    inputs.file(webIconsSvg)
    outputs.files(
        webIconsDir.resolve("icon-192.png"),
        webIconsDir.resolve("icon-512.png"),
    )
    doLast {
        listOf(192, 512).forEach { size ->
            val exit = ProcessBuilder(
                "rsvg-convert",
                "-w", size.toString(),
                "-h", size.toString(),
                webIconsSvg.absolutePath,
                "-o", webIconsDir.resolve("icon-$size.png").absolutePath,
            ).redirectErrorStream(true).start().waitFor()
            check(exit == 0) { "rsvg-convert failed for size $size with exit code $exit" }
        }
    }
}

// A single file build of the page: the scripts are copied into index.html so that publishing
// means publishing one document, with no separate requests to get wrong and no chance of a
// cached page running against a newer script. The digits font goes in as well, as a data URL.
//
// It is a separate directory rather than an edit in place, so the normal distribution is left
// as it is and this step stays repeatable. Only the JS target is handled: the Wasm build loads
// a .wasm file of its own, which has to remain a separate request, while the JS bundle is
// self-contained.
val webStandaloneDir = layout.buildDirectory.dir("dist/js/standalone")

// Four kilobytes, small enough to travel inside the page.
val webFont = "MarkaziTextDigits-VariableFont_wght.ttf"

val webStandalone = tasks.register<Sync>("webStandalone") {
    description = "Copy the JS distribution to a directory whose index.html holds the scripts"
    dependsOn("jsBrowserDistribution")
    from(layout.buildDirectory.dir("dist/js/productionExecutable"))
    into(webStandaloneDir)
    doLast {
        val dir = webStandaloneDir.get().asFile
        val html = dir.resolve("index.html")
        var text = html.readText()

        // In the order they are listed in the page: main.js defines the hook the bundle calls.
        listOf("main.js", "web.js").forEach { name ->
            val script = dir.resolve(name)
            check(script.isFile) { "$name is missing from the distribution" }
            val code = script.readText()
                // A source map that is no longer a file of its own.
                .replace(Regex("(?m)^//# sourceMappingURL=.*\\n?"), "")
                // Would close the tag early wherever the bundle happens to hold the string.
                .replace("</script", "<\\/script")
            val tag = Regex("""[ \t]*<script src="$name"></script>\n""")
            check(tag.containsMatchIn(text)) { "index.html has no <script src=\"$name\">" }
            text = text.replace(tag) { "<script>\n$code\t</script>\n" }
        }

        // The font is the one asset left outside the document, and base64 is what makes it
        // travel with it: nothing to request, nothing to fail, nothing for a stale cache to
        // hold on to. It is swapped in for the file name the page asks for, in place.
        val font = dir.resolve(webFont)
        check(font.isFile) { "$webFont is missing from the distribution" }
        val fontData = Base64.getEncoder().encodeToString(font.readBytes())
        val fontUrl = "url('$webFont')"
        check(text.contains(fontUrl)) { "index.html has no $fontUrl" }
        text = text.replace(fontUrl, "url('data:font/ttf;base64,$fontData')")

        html.writeText(text)

        // The files that are inside the page now are neither published nor precached.
        val inlined = listOf("main.js", "web.js", webFont)
        inlined.forEach { dir.resolve(it).delete() }
        listOf("main.js.map", "web.js.map").forEach { dir.resolve(it).delete() }
        val worker = dir.resolve("sw.js")
        if (worker.isFile) {
            // The BUILD stamped into sw.js is left alone here: the deploy workflow puts the
            // commit in its place, since that is the only place where a revision is certain.
            worker.writeText(
                worker.readText().lines()
                    .filterNot { line -> inlined.any { line.contains("'./$it'") } }
                    .joinToString("\n")
            )
        }
    }
}
