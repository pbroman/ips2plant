package com.github.pbroman.ips2plant.ui.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyManagerTest {

    // DependencyManager needs a Project and ModelDirTreeManager, but findPomDir and
    // collectProjectArtifactIds only use file-system paths — we access them directly
    // via package-private visibility.

    private DependencyManager manager;

    @BeforeEach
    void setUp() {
        // Pass nulls — the methods under test do not use project or treeManager
        manager = new DependencyManager(null, null);
    }

    // --- findPomDir ---

    @Test
    void findPomDir_pomInDirectParent_returnsParent(@TempDir Path root) throws IOException {
        var module = root.resolve("module");
        var model = module.resolve("model");
        Files.createDirectories(model);
        Files.createFile(module.resolve("pom.xml"));

        assertThat(manager.findPomDir(model)).isEqualTo(module);
    }

    @Test
    void findPomDir_pomInGrandparent_returnsGrandparent(@TempDir Path root) throws IOException {
        var module = root.resolve("module");
        var sub = module.resolve("sub");
        var model = sub.resolve("model");
        Files.createDirectories(model);
        Files.createFile(module.resolve("pom.xml"));

        assertThat(manager.findPomDir(model)).isEqualTo(module);
    }

    @Test
    void findPomDir_noPomAnywhere_returnsNull(@TempDir Path root) throws IOException {
        var model = root.resolve("module").resolve("model");
        Files.createDirectories(model);

        assertThat(manager.findPomDir(model)).isNull();
    }

    @Test
    void findPomDir_pomInSameDir_notFound(@TempDir Path root) throws IOException {
        // findPomDir starts at modelDir.getParent(), so pom.xml in modelDir itself is skipped
        var model = root.resolve("model");
        Files.createDirectories(model);
        Files.createFile(model.resolve("pom.xml"));

        assertThat(manager.findPomDir(model)).isNull();
    }

    @Test
    void findPomDir_pomAsDirectory_notFound(@TempDir Path root) throws IOException {
        var module = root.resolve("module");
        var model = module.resolve("model");
        Files.createDirectories(model);
        Files.createDirectories(module.resolve("pom.xml")); // directory, not file

        assertThat(manager.findPomDir(model)).isNull();
    }

    // --- collectProjectArtifactIds ---

    @Test
    void collectProjectArtifactIds_singlePom_returnsArtifactId(@TempDir Path root) throws IOException {
        writePom(root.resolve("pom.xml"), "my-module", null, null);

        assertThat(manager.collectProjectArtifactIds(root)).containsExactly("my-module");
    }

    @Test
    void collectProjectArtifactIds_multiplePoms_returnsAll(@TempDir Path root) throws IOException {
        writePom(root.resolve("pom.xml"), "parent-module", null, null);
        var sub = root.resolve("sub");
        Files.createDirectories(sub);
        writePom(sub.resolve("pom.xml"), "child-module", null, null);

        assertThat(manager.collectProjectArtifactIds(root))
                .containsExactlyInAnyOrder("parent-module", "child-module");
    }

    @Test
    void collectProjectArtifactIds_parentBlockExcluded(@TempDir Path root) throws IOException {
        // The parent's artifactId inside <parent>...</parent> should not be extracted
        writePom(root.resolve("pom.xml"), "my-module", "parent-artifact", null);

        assertThat(manager.collectProjectArtifactIds(root))
                .containsExactly("my-module")
                .doesNotContain("parent-artifact");
    }

    @Test
    void collectProjectArtifactIds_artifactIdAfterDependencies_notIncluded(@TempDir Path root) throws IOException {
        // artifactIds that appear only inside <dependencies> are not extracted
        writePom(root.resolve("pom.xml"), "my-module", null, "dep-artifact");

        assertThat(manager.collectProjectArtifactIds(root))
                .containsExactly("my-module")
                .doesNotContain("dep-artifact");
    }

    @Test
    void collectProjectArtifactIds_emptyDirectory_returnsEmpty(@TempDir Path root) {
        assertThat(manager.collectProjectArtifactIds(root)).isEmpty();
    }

    // --- helpers ---

    private void writePom(Path file, String artifactId, String parentArtifactId, String depArtifactId)
            throws IOException {
        var sb = new StringBuilder();
        sb.append("<project>\n");
        if (parentArtifactId != null) {
            sb.append("  <parent><artifactId>").append(parentArtifactId).append("</artifactId></parent>\n");
        }
        sb.append("  <artifactId>").append(artifactId).append("</artifactId>\n");
        if (depArtifactId != null) {
            sb.append("  <dependencies>\n");
            sb.append("    <dependency><artifactId>").append(depArtifactId).append("</artifactId></dependency>\n");
            sb.append("  </dependencies>\n");
        }
        sb.append("</project>\n");
        Files.writeString(file, sb.toString());
    }
}
