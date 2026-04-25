plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.14.0"
}

group = "com.github.pbroman"
version = "0.4.2"

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
            intellijIdeaCommunity("2024.1")
        }
    }
    implementation("net.sf.saxon:Saxon-HE:12.9")

    testImplementation(platform("org.junit:junit-bom:5.14.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
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
            sinceBuild = "241"
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
    compileJava {
        options.compilerArgs.add("-Xlint:deprecation")
    }
}