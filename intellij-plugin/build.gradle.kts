plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "com.github.pbroman"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        instrumentationTools()
        intellijIdeaCommunity("2024.1")
    }
    implementation("net.sf.saxon:Saxon-HE:12.5")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.3")
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
        description = "Generates PlantUML class diagrams from Faktor-IPS model files"
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
    buildSearchableOptions {
        enabled = false
    }
}