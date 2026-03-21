package com.github.pbroman.ips2plant.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class IpsProjectDetectorTest {

    private final IpsProjectDetector detector = new IpsProjectDetector();

    @TempDir
    Path tempDir;

    @Test
    void detectModelDirs_noIpsProjectFile_returnsEmpty() {
        // given
        // empty temp directory

        // when
        var dirs = detector.detectModelDirs(tempDir);

        // then
        assertThat(dirs).isEmpty();
    }

    @Test
    void detectModelDirs_withSrcEntry_returnsModelDir() throws IOException {
        // given
        var modelDir = tempDir.resolve("model");
        Files.createDirectories(modelDir);
        writeIpsProject(tempDir, """
                <?xml version="1.0" encoding="UTF-8"?>
                <IpsProject>
                    <IpsObjectPath>
                        <Entry type="src" sourceFolder="model"/>
                    </IpsObjectPath>
                </IpsProject>
                """);

        // when
        var dirs = detector.detectModelDirs(tempDir);

        // then
        assertThat(dirs).containsExactly(modelDir);
    }

    @Test
    void detectModelDirs_multipleSourceFolders_returnsAll() throws IOException {
        // given
        var model1 = tempDir.resolve("model");
        var model2 = tempDir.resolve("productmodel");
        Files.createDirectories(model1);
        Files.createDirectories(model2);
        writeIpsProject(tempDir, """
                <?xml version="1.0" encoding="UTF-8"?>
                <IpsProject>
                    <IpsObjectPath>
                        <Entry type="src" sourceFolder="model"/>
                        <Entry type="src" sourceFolder="productmodel"/>
                    </IpsObjectPath>
                </IpsProject>
                """);

        // when
        var dirs = detector.detectModelDirs(tempDir);

        // then
        assertThat(dirs).containsExactlyInAnyOrder(model1, model2);
    }

    @Test
    void detectModelDirs_nonSrcEntries_areIgnored() throws IOException {
        // given
        writeIpsProject(tempDir, """
                <?xml version="1.0" encoding="UTF-8"?>
                <IpsProject>
                    <IpsObjectPath>
                        <Entry type="container" id="org.faktorips.javacontainer"/>
                    </IpsObjectPath>
                </IpsProject>
                """);

        // when
        var dirs = detector.detectModelDirs(tempDir);

        // then
        assertThat(dirs).isEmpty();
    }

    @Test
    void detectModelDirs_sourceFolderDoesNotExist_isSkipped() throws IOException {
        // given
        writeIpsProject(tempDir, """
                <?xml version="1.0" encoding="UTF-8"?>
                <IpsProject>
                    <IpsObjectPath>
                        <Entry type="src" sourceFolder="nonexistent"/>
                    </IpsObjectPath>
                </IpsProject>
                """);

        // when
        var dirs = detector.detectModelDirs(tempDir);

        // then
        assertThat(dirs).isEmpty();
    }

    @Test
    void detectModelDirs_nestedProject_detected() throws IOException {
        // given
        var subProject = tempDir.resolve("sub/project");
        var modelDir = subProject.resolve("model");
        Files.createDirectories(modelDir);
        writeIpsProject(subProject, """
                <?xml version="1.0" encoding="UTF-8"?>
                <IpsProject>
                    <IpsObjectPath>
                        <Entry type="src" sourceFolder="model"/>
                    </IpsObjectPath>
                </IpsProject>
                """);

        // when
        var dirs = detector.detectModelDirs(tempDir);

        // then
        assertThat(dirs).containsExactly(modelDir);
    }

    @Test
    void detectModelDirs_malformedXml_isSkipped() throws IOException {
        // given
        Files.writeString(tempDir.resolve(".ipsproject"), "not valid xml <<<<");

        // when
        var dirs = detector.detectModelDirs(tempDir);

        // then
        assertThat(dirs).isEmpty();
    }

    private void writeIpsProject(Path dir, String content) throws IOException {
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(".ipsproject"), content);
    }
}