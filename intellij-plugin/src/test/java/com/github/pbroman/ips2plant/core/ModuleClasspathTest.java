package com.github.pbroman.ips2plant.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleClasspathTest {

    @TempDir
    Path tempDir;

    private Path module(String name, String... deps) throws IOException {
        var moduleDir = Files.createDirectories(tempDir.resolve(name));
        var depXml = new StringBuilder();
        for (var dep : deps) {
            var parts = dep.split(":");
            depXml.append("<dependency><artifactId>").append(parts[0]).append("</artifactId>")
                    .append(parts.length > 1 ? "<scope>" + parts[1] + "</scope>" : "").append("</dependency>");
        }
        Files.writeString(moduleDir.resolve("pom.xml"),
                "<project><artifactId>" + name + "</artifactId><dependencies>" + depXml + "</dependencies></project>");
        return Files.createDirectories(moduleDir.resolve("model"));
    }

    @Test
    void visibleDirs_includesOwnAndTransitiveCompileDependenciesOnly() throws IOException {
        // given
        var shared = module("shared");
        var ind = module("ind", "shared");
        var lia = module("lia", "ind", "shared");
        var prp = module("prp", "ind");
        var all = List.of(shared, ind, lia, prp);

        // when
        var visible = ModuleClasspath.visibleDirs(lia, all, Map.of());

        // then
        assertThat(visible).get().asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.COLLECTION)
                .containsExactlyInAnyOrder(lia, ind, shared);
    }

    @Test
    void visibleDirs_excludesTestAndRuntimeScopes() throws IOException {
        // given
        var ind = module("ind");
        var runtimeOnly = module("rt");
        var lia = module("lia", "ind:test", "rt:runtime");

        // when
        var visible = ModuleClasspath.visibleDirs(lia, List.of(ind, runtimeOnly, lia), Map.of());

        // then
        assertThat(visible).get().asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.COLLECTION)
                .containsExactly(lia);
    }

    @Test
    void visibleDirs_includesResolvedJarDependenciesByArtifactId() throws IOException {
        // given
        var lia = module("lia", "ext-lib");
        var jarDir = Files.createDirectories(tempDir.resolve("jar/model"));
        var otherJarDir = Files.createDirectories(tempDir.resolve("other/model"));

        // when
        var visible = ModuleClasspath.visibleDirs(lia, List.of(lia),
                Map.of(jarDir, "de.faktorzehn:ext-lib", otherJarDir, "de.faktorzehn:unrelated"));

        // then
        assertThat(visible).get().asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.COLLECTION)
                .containsExactlyInAnyOrder(lia, jarDir);
    }

    @Test
    void visibleDirs_followsDependenciesDeclaredInParentPom() throws IOException {
        // given
        var ind = module("ind");
        var child = Files.createDirectories(tempDir.resolve("parent/child/model"));
        Files.writeString(tempDir.resolve("parent/pom.xml"),
                "<project><artifactId>parent</artifactId><dependencies><dependency><artifactId>ind</artifactId>"
                        + "</dependency></dependencies></project>");
        Files.writeString(tempDir.resolve("parent/child/pom.xml"),
                "<project><parent><artifactId>parent</artifactId></parent><artifactId>child</artifactId></project>");

        // when
        var visible = ModuleClasspath.visibleDirs(child, List.of(ind, child), Map.of());

        // then
        assertThat(visible).get().asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.COLLECTION)
                .containsExactlyInAnyOrder(child, ind);
    }

    @Test
    void visibleDirs_isEmptyWithoutPom() throws IOException {
        // given
        var orphan = Files.createDirectories(tempDir.resolve("nopom/model"));

        // when / then
        assertThat(ModuleClasspath.visibleDirs(orphan, List.of(orphan), Map.of())).isEmpty();
    }
}
