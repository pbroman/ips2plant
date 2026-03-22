package com.github.pbroman.ips2plant.core;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class MavenDependencyCollectorTest {

    private final MavenDependencyCollector collector = new MavenDependencyCollector();

    @TempDir
    Path tempDir;

    @Test
    void parseClasspath_empty_returnsEmptyList() {
        assertThat(collector.parseClasspath("")).isEmpty();
        assertThat(collector.parseClasspath(null)).isEmpty();
        assertThat(collector.parseClasspath("   ")).isEmpty();
    }

    @Test
    void parseClasspath_singleJar_returnsList() throws IOException {
        var jar = createDummyJar("test.jar");
        var result = collector.parseClasspath(jar.toString());
        assertThat(result).containsExactly(jar.toString());
    }

    @Test
    void parseClasspath_multipleJars_returnsList() throws IOException {
        var jar1 = createDummyJar("a.jar");
        var jar2 = createDummyJar("b.jar");
        var classpath = jar1 + java.io.File.pathSeparator + jar2;
        var result = collector.parseClasspath(classpath);
        assertThat(result).containsExactly(jar1.toString(), jar2.toString());
    }

    @Test
    void parseClasspath_nonExistentJar_isSkipped() {
        var result = collector.parseClasspath("/nonexistent/path/foo.jar");
        assertThat(result).isEmpty();
    }

    @Test
    void extractIpsFilesFromJar_jarWithIpsFiles_extractsThem() throws IOException {
        var jarPath = createJarWithIpsFiles();

        var result = collector.extractIpsFilesFromJar(jarPath.toString());

        assertThat(result).isNotNull();
        assertThat(result.toString()).endsWith("model");

        // Check extracted files
        var contractFile = result.resolve("contract/Contract.ipspolicycmpttype");
        assertThat(contractFile).exists();
        assertThat(Files.readString(contractFile)).contains("PolicyCmptType");

        var enumFile = result.resolve("enums/Status.ipsenumtype");
        assertThat(enumFile).exists();
    }

    @Test
    void extractIpsFilesFromJar_jarWithoutIpsFiles_returnsNull() throws IOException {
        var jarPath = createJarWithoutIpsFiles();
        var result = collector.extractIpsFilesFromJar(jarPath.toString());
        assertThat(result).isNull();
    }

    @Test
    void extractIpsFilesFromJar_nonExistentJar_returnsNull() throws IOException {
        var result = collector.extractIpsFilesFromJar("/nonexistent/file.jar");
        assertThat(result).isNull();
    }

    @Test
    void extractIpsFilesFromJar_ignoresNonModelIpsFiles() throws IOException {
        var jarPath = tempDir.resolve("non-model.jar");
        try (var jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            // IPS file NOT under model/ prefix
            addJarEntry(jos, "src/main/resources/Something.ipsenumtype", "<EnumType/>");
        }

        var result = collector.extractIpsFilesFromJar(jarPath.toString());
        assertThat(result).isNull();
    }

    private Path createJarWithIpsFiles() throws IOException {
        var jarPath = tempDir.resolve("with-ips.jar");
        try (var jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            addJarEntry(jos, "model/contract/Contract.ipspolicycmpttype",
                    "<PolicyCmptType/>");
            addJarEntry(jos, "model/enums/Status.ipsenumtype",
                    "<EnumType/>");
            addJarEntry(jos, "de/example/SomeClass.class",
                    "bytecode");
        }
        return jarPath;
    }

    private Path createJarWithoutIpsFiles() throws IOException {
        var jarPath = tempDir.resolve("no-ips.jar");
        try (var jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            addJarEntry(jos, "de/example/SomeClass.class", "bytecode");
            addJarEntry(jos, "META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n");
        }
        return jarPath;
    }

    @ParameterizedTest
    @CsvSource({
            "/repo/com/example/my-artifact-1.2.3.jar, my-artifact",
            "/repo/org/foo/bar-baz-2.0.0-SNAPSHOT.jar, bar-baz",
            "/repo/some-lib-0.1.jar, some-lib",
            "plain.jar, plain"
    })
    void extractArtifactId_variousFormats(String jarPath, String expectedArtifactId) {
        assertThat(MavenDependencyCollector.extractArtifactId(jarPath)).isEqualTo(expectedArtifactId);
    }

    private Path createDummyJar(String name) throws IOException {
        var jarPath = tempDir.resolve(name);
        try (var jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            addJarEntry(jos, "META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n");
        }
        return jarPath;
    }

    private void addJarEntry(JarOutputStream jos, String name, String content) throws IOException {
        jos.putNextEntry(new JarEntry(name));
        jos.write(content.getBytes());
        jos.closeEntry();
    }
}
