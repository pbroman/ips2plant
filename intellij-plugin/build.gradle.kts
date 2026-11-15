plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.14.0"
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
    implementation("net.sf.saxon:Saxon-HE:13.0")

    testImplementation(platform("org.junit:junit-bom:5.14.3"))
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
    pluginConfiguration {
        id = "com.github.pbroman.ips2plant"
        name = "IPS to PlantUML"
        version = project.version.toString()
        description = """
            Generates PlantUML class diagrams from Faktor-IPS model files (.ipspolicycmpttype, .ipsproductcmpttype, .ipsenumtype, .ipsenumcontent, .ipstablestructure).
            <br/>
            <ul>
                <li>Tool window with model directory tree, search, and diagram options</li>
                <li>Resolve IPS model files from Maven dependency JARs</li>
                <li>Opens result directly in the editor</li>
            </ul>
        """
        vendor {
            name = "pbroman"
        }
        ideaVersion {
            sinceBuild = "261"
            untilBuild = provider { null }
        }
    }
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

    jarSearchableOptions {
        enabled = false
    }
    buildSearchableOptions {
        enabled = false
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