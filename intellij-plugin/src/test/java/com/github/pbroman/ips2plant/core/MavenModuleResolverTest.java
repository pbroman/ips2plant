package com.github.pbroman.ips2plant.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class MavenModuleResolverTest {

    private final MavenModuleResolver resolver = new MavenModuleResolver();

    @TempDir
    Path tempDir;

    @Test
    void resolveModule_pomWithGroupIdAndArtifactId_returnsBoth() throws IOException {
        // given
        var pomContent = """
                <?xml version="1.0"?>
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>my-module</artifactId>
                </project>
                """;
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);
        var file = Files.createFile(tempDir.resolve("SomeFile.xml"));

        // when
        var result = resolver.resolveModule(file.toFile());

        // then
        assertThat(result).isEqualTo("com.example:my-module");
    }

    @Test
    void resolveModule_groupIdInheritedFromParent_usesParentGroupId() throws IOException {
        // given
        var pomContent = """
                <?xml version="1.0"?>
                <project>
                    <parent>
                        <groupId>com.parent</groupId>
                        <artifactId>parent-pom</artifactId>
                    </parent>
                    <artifactId>child-module</artifactId>
                </project>
                """;
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);
        var file = Files.createFile(tempDir.resolve("SomeFile.xml"));

        // when
        var result = resolver.resolveModule(file.toFile());

        // then
        assertThat(result).isEqualTo("com.parent:child-module");
    }

    @Test
    void resolveModule_ownGroupIdOverridesParent_usesOwnGroupId() throws IOException {
        // given
        var pomContent = """
                <?xml version="1.0"?>
                <project>
                    <parent>
                        <groupId>com.parent</groupId>
                        <artifactId>parent-pom</artifactId>
                    </parent>
                    <groupId>com.own</groupId>
                    <artifactId>my-module</artifactId>
                </project>
                """;
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);
        var file = Files.createFile(tempDir.resolve("SomeFile.xml"));

        // when
        var result = resolver.resolveModule(file.toFile());

        // then
        assertThat(result).isEqualTo("com.own:my-module");
    }

    @Test
    void resolveModule_noGroupIdAnywhere_returnsArtifactIdOnly() throws IOException {
        // given
        var pomContent = """
                <?xml version="1.0"?>
                <project>
                    <artifactId>standalone</artifactId>
                </project>
                """;
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);
        var file = Files.createFile(tempDir.resolve("SomeFile.xml"));

        // when
        var result = resolver.resolveModule(file.toFile());

        // then
        assertThat(result).isEqualTo("standalone");
    }

    @Test
    void resolveModule_noPomXml_returnsNull() throws IOException {
        // given
        var file = Files.createFile(tempDir.resolve("SomeFile.xml"));

        // when
        var result = resolver.resolveModule(file.toFile());

        // then
        assertThat(result).isNull();
    }

    @Test
    void resolveModule_pomInParentDir_walksUpToFindIt() throws IOException {
        // given
        var pomContent = """
                <?xml version="1.0"?>
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>parent-module</artifactId>
                </project>
                """;
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);
        var subDir = Files.createDirectories(tempDir.resolve("src/main/model"));
        var file = Files.createFile(subDir.resolve("SomeFile.xml"));

        // when
        var result = resolver.resolveModule(file.toFile());

        // then
        assertThat(result).isEqualTo("com.example:parent-module");
    }

    @Test
    void resolveModule_ignoresDependencyArtifactIds() throws IOException {
        // given
        var pomContent = """
                <?xml version="1.0"?>
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>my-module</artifactId>
                    <dependencies>
                        <dependency>
                            <groupId>com.other</groupId>
                            <artifactId>should-not-match</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """;
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);
        var file = Files.createFile(tempDir.resolve("SomeFile.xml"));

        // when
        var result = resolver.resolveModule(file.toFile());

        // then
        assertThat(result).isEqualTo("com.example:my-module");
    }

    @Test
    void resolveModule_cachesResult_sameResultOnSecondCall() throws IOException {
        // given
        var pomContent = """
                <?xml version="1.0"?>
                <project>
                    <groupId>com.cached</groupId>
                    <artifactId>cached-module</artifactId>
                </project>
                """;
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);
        var file1 = Files.createFile(tempDir.resolve("A.xml"));
        var file2 = Files.createFile(tempDir.resolve("B.xml"));

        // when
        var result1 = resolver.resolveModule(file1.toFile());
        var result2 = resolver.resolveModule(file2.toFile());

        // then
        assertThat(result1).isEqualTo("com.cached:cached-module");
        assertThat(result2).isEqualTo(result1);
    }

    @Test
    void resolveAll_multipleFiles_resolvesEach() throws IOException {
        // given
        var pomContent = """
                <?xml version="1.0"?>
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>test-module</artifactId>
                </project>
                """;
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);
        var fileA = Files.createFile(tempDir.resolve("A.xml")).toFile();
        var fileB = Files.createFile(tempDir.resolve("B.xml")).toFile();

        // when
        var result = resolver.resolveAll(Map.of("com.A", fileA, "com.B", fileB));

        // then
        assertThat(result).containsEntry("com.A", "com.example:test-module")
                .containsEntry("com.B", "com.example:test-module");
    }

    @Test
    void registerModule_registeredPath_takePrecedenceOverPom() throws IOException {
        // given
        var pomContent = """
                <?xml version="1.0"?>
                <project>
                    <groupId>com.pom</groupId>
                    <artifactId>pom-module</artifactId>
                </project>
                """;
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);
        var file = Files.createFile(tempDir.resolve("SomeFile.xml"));
        resolver.registerModule(tempDir, "com.registered:registered-module");

        // when
        var result = resolver.resolveModule(file.toFile());

        // then
        assertThat(result).isEqualTo("com.registered:registered-module");
    }

    @Test
    void registerModule_nonMatchingPath_fallsBackToPom() throws IOException {
        // given
        var pomContent = """
                <?xml version="1.0"?>
                <project>
                    <groupId>com.pom</groupId>
                    <artifactId>pom-module</artifactId>
                </project>
                """;
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);
        var file = Files.createFile(tempDir.resolve("SomeFile.xml"));
        resolver.registerModule(Path.of("/some/other/path"), "com.other:other-module");

        // when
        var result = resolver.resolveModule(file.toFile());

        // then
        assertThat(result).isEqualTo("com.pom:pom-module");
    }
}