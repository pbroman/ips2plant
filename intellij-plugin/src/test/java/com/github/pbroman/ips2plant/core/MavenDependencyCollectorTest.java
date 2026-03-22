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
        // given / when / then
        assertThat(collector.parseClasspath("")).isEmpty();
        assertThat(collector.parseClasspath(null)).isEmpty();
        assertThat(collector.parseClasspath("   ")).isEmpty();
    }

    @Test
    void parseClasspath_singleJar_returnsList() throws IOException {
        // given
        var jar = createDummyJar("test.jar");

        // when
        var result = collector.parseClasspath(jar.toString());

        // then
        assertThat(result).containsExactly(jar.toString());
    }

    @Test
    void parseClasspath_multipleJars_returnsList() throws IOException {
        // given
        var jar1 = createDummyJar("a.jar");
        var jar2 = createDummyJar("b.jar");
        var classpath = jar1 + java.io.File.pathSeparator + jar2;

        // when
        var result = collector.parseClasspath(classpath);

        // then
        assertThat(result).containsExactly(jar1.toString(), jar2.toString());
    }

    @Test
    void parseClasspath_nonExistentJar_isSkipped() {
        // given
        var classpath = "/nonexistent/path/foo.jar";

        // when
        var result = collector.parseClasspath(classpath);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void extractIpsFilesFromJar_jarWithIpsFiles_extractsThem() throws IOException {
        // given
        var jarPath = createJarWithIpsFiles();

        // when
        var result = collector.extractIpsFilesFromJar(jarPath.toString());

        // then
        assertThat(result).isNotNull();
        assertThat(result.toString()).endsWith("model");

        var contractFile = result.resolve("contract/Contract.ipspolicycmpttype");
        assertThat(contractFile).exists();
        assertThat(Files.readString(contractFile)).contains("PolicyCmptType");

        var enumFile = result.resolve("enums/Status.ipsenumtype");
        assertThat(enumFile).exists();
    }

    @Test
    void extractIpsFilesFromJar_jarWithoutIpsFiles_returnsNull() throws IOException {
        // given
        var jarPath = createJarWithoutIpsFiles();

        // when
        var result = collector.extractIpsFilesFromJar(jarPath.toString());

        // then
        assertThat(result).isNull();
    }

    @Test
    void extractIpsFilesFromJar_nonExistentJar_returnsNull() throws IOException {
        // given
        var jarPath = "/nonexistent/file.jar";

        // when
        var result = collector.extractIpsFilesFromJar(jarPath);

        // then
        assertThat(result).isNull();
    }

    @Test
    void extractIpsFilesFromJar_ignoresNonModelIpsFiles() throws IOException {
        // given: IPS file NOT under model/ prefix
        var jarPath = tempDir.resolve("non-model.jar");
        try (var jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            addJarEntry(jos, "src/main/resources/Something.ipsenumtype", "<EnumType/>");
        }

        // when
        var result = collector.extractIpsFilesFromJar(jarPath.toString());

        // then
        assertThat(result).isNull();
    }

    @Test
    void extractIpsFilesFromJar_ignoresUnsupportedIpsTypes() throws IOException {
        // given: JAR with unsupported IPS file types under model/
        var jarPath = tempDir.resolve("unsupported-ips.jar");
        try (var jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            addJarEntry(jos, "model/something/Src.ipssrcfile", "<IpsSrcFile/>");
            addJarEntry(jos, "model/something/Test.ipstest", "<Test/>");
            addJarEntry(jos, "model/something/Prod.ipsproductcmpt", "<ProductCmpt/>");
        }

        // when
        var result = collector.extractIpsFilesFromJar(jarPath.toString());

        // then
        assertThat(result).isNull();
    }

    @Test
    void extractIpsFilesFromJar_extractsOnlySupportedTypes() throws IOException {
        // given: JAR with both supported and unsupported IPS file types
        var jarPath = tempDir.resolve("mixed-ips.jar");
        try (var jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            addJarEntry(jos, "model/contract/Contract.ipspolicycmpttype", "<PolicyCmptType/>");
            addJarEntry(jos, "model/contract/Src.ipssrcfile", "<IpsSrcFile/>");
        }

        // when
        var result = collector.extractIpsFilesFromJar(jarPath.toString());

        // then
        assertThat(result).isNotNull();
        assertThat(result.resolve("contract/Contract.ipspolicycmpttype")).exists();
        assertThat(result.resolve("contract/Src.ipssrcfile")).doesNotExist();
    }

    @ParameterizedTest
    @CsvSource({
            "/repo/com/example/my-artifact-1.2.3.jar, my-artifact",
            "/repo/org/foo/bar-baz-2.0.0-SNAPSHOT.jar, bar-baz",
            "/repo/some-lib-0.1.jar, some-lib",
            "plain.jar, plain"
    })
    void extractArtifactId_variousFormats(String jarPath, String expectedArtifactId) {
        // given / when / then
        assertThat(MavenDependencyCollector.extractArtifactId(jarPath)).isEqualTo(expectedArtifactId);
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