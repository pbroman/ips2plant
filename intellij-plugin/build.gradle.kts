import org.jetbrains.changelog.markdownToHTML

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.20.1"
    // only for markdownToHTML (description and change notes); the changelog tasks are not used
    id("org.jetbrains.changelog") version "2.5.0" apply false
}

group = "com.github.pbroman"
version = "0.5.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        val localIde = providers.gradleProperty("localIde").orNull
        if (localIde != null) {
            local(localIde)
        } else {
            intellijIdea("2026.1")
        }
    }
    implementation("net.sf.saxon:Saxon-HE:13.1")

    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    // Running tests loads IntelliJ Platform runtime classes from the local IDE, which may be
    // built for a newer class file version than the project's Java 21 target (see compileJava).
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(25))
    })
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

intellijPlatform {
    // Disables buildSearchableOptions, prepareJarSearchableOptions and jarSearchableOptions together
    buildSearchableOptions = false

    pluginConfiguration {
        id = "com.github.pbroman.ips2plant"
        name = "IPS to PlantUML"
        version = project.version.toString()
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            markdownToHTML(pluginDescription(it))
        }
        changeNotes = providers.fileContents(layout.projectDirectory.file("../RELEASE_NOTES.md")).asText.map {
            markdownToHTML(pluginChangeNotes(it, project.version.toString()))
        }
        vendor {
            name = "pbroman"
            url = "https://github.com/pbroman"
        }
        ideaVersion {
            sinceBuild = "261"
            untilBuild = provider { null }
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

/** The part of the README between the plugin description markers; the build fails if they are missing. */
fun pluginDescription(readme: String): String {
    val start = "<!-- Plugin description -->"
    val end = "<!-- Plugin description end -->"
    require(readme.contains(start) && readme.contains(end)) { "README.md: plugin description markers missing" }
    return readme.substringAfter(start).substringBefore(end).trim()
}

/**
 * The section of the release notes for this version, without its heading and without subsections that only
 * concern the Quarkus CLI. Falls back to a link if the version has no section yet.
 */
fun pluginChangeNotes(releaseNotes: String, version: String): String {
    val link = "Full release notes: [RELEASE_NOTES.md](https://github.com/pbroman/ips2plant/blob/main/RELEASE_NOTES.md)"
    val section = releaseNotes.lines()
        .dropWhile { it.trim() != "## $version" && !it.startsWith("## $version ") }
        .drop(1)
        .takeWhile { !it.startsWith("## ") }
        .joinToString("\n")
    if (section.isBlank()) {
        return link
    }
    val subsections = ("\n" + section).split("\n### ")
    val kept = subsections.drop(1)
        .filterNot { it.startsWith("Quarkus CLI") }
        .joinToString("\n\n") { "### " + it.trim() }
    return (subsections[0].trim() + "\n\n" + kept).trim() + "\n\n" + link
}

tasks {
    // XSL files are authored in the Quarkus module (source of truth) and copied here at build time.
    // The destination directory is git-ignored — do not edit the XSL files under src/main/resources/xsl/
    // directly; edit them in ../quarkus/src/main/resources/xsl/ instead.
    register<Copy>("syncXsl") {
        from("../quarkus/src/main/resources/xsl")
        into("src/main/resources/xsl")
    }

    // syncXsl must run before resources are processed so the copied files are included in the plugin JAR
    processResources {
        dependsOn("syncXsl")
    }

    compileJava {
        options.compilerArgs.add("-Xlint:deprecation")
    }
    // Toolbox auto-updates the local IDE in place, so its bundled platform jars may require a
    // newer class file version than the project's Java 21 target. Use a newer compiler to read
    // them while still emitting Java 21 bytecode.
    withType<JavaCompile>().configureEach {
        javaCompiler.set(project.javaToolchains.compilerFor {
            languageVersion.set(JavaLanguageVersion.of(25))
        })
    }
}