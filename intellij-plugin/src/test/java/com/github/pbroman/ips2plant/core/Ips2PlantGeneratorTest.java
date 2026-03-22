package com.github.pbroman.ips2plant.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class Ips2PlantGeneratorTest {

    private final Ips2PlantGenerator generator = new Ips2PlantGenerator();

    @TempDir
    Path tempDir;

    @Test
    void generate_emptyDirectory_returnsNoteDiagram() {
        // given
        var options = new Ips2PlantOptions();

        // when
        var result = generator.generate(List.of(tempDir), options);

        // then
        assertThat(result).contains("@startuml")
                .contains("No IPS model files found")
                .contains("@enduml");
    }

    @Test
    void generate_withPolicyCmptType_producesPlantUml() throws IOException {
        // given
        var file = tempDir.resolve("Contract.ipspolicycmpttype");
        Files.writeString(file, """
                <?xml version="1.0"?>
                <PolicyCmptType supertype="">
                    <Attribute name="premium" attributeType="changeable" datatype="Money"/>
                </PolicyCmptType>
                """);
        var options = new Ips2PlantOptions();

        // when
        var result = generator.generate(List.of(tempDir), options);

        // then
        assertThat(result).startsWith("@startuml")
                .contains("Contract")
                .contains("premium")
                .endsWith("@enduml");
    }

    @Test
    void generate_multipleDirectories_combinedDiagram() throws IOException {
        // given
        var dir1 = tempDir.resolve("dir1");
        var dir2 = tempDir.resolve("dir2");
        Files.createDirectories(dir1);
        Files.createDirectories(dir2);
        Files.writeString(dir1.resolve("A.ipspolicycmpttype"),
                "<?xml version=\"1.0\"?><PolicyCmptType/>");
        Files.writeString(dir2.resolve("B.ipspolicycmpttype"),
                "<?xml version=\"1.0\"?><PolicyCmptType/>");
        var options = new Ips2PlantOptions();

        // when
        var result = generator.generate(List.of(dir1, dir2), options);

        // then
        assertThat(result).contains("A")
                .contains("B");
    }

    @Test
    void generate_dependencyFiltering_includesDirectlyReferencedTypes() throws IOException {
        // given: local type references a dependency type via supertype
        var localDir = tempDir.resolve("local");
        var depDir = tempDir.resolve("dep");
        Files.createDirectories(localDir);
        Files.createDirectories(depDir);
        Files.writeString(localDir.resolve("Child.ipspolicycmpttype"),
                "<?xml version=\"1.0\"?><PolicyCmptType supertype=\"Base\"/>");
        Files.writeString(depDir.resolve("Base.ipspolicycmpttype"),
                "<?xml version=\"1.0\"?><PolicyCmptType/>");
        Files.writeString(depDir.resolve("Unrelated.ipspolicycmpttype"),
                "<?xml version=\"1.0\"?><PolicyCmptType/>");
        var options = new Ips2PlantOptions();

        // when
        var result = generator.generate(List.of(localDir), List.of(depDir), options, msg -> {});

        // then: Base included, Unrelated excluded
        assertThat(result).contains("Child").contains("Base").doesNotContain("Unrelated");
    }

    @Test
    void generate_dependencyFiltering_includesTransitivelyReferencedTypes() throws IOException {
        // given: local -> DepA (via supertype) -> DepB (via supertype)
        var localDir = tempDir.resolve("local");
        var depDir = tempDir.resolve("dep");
        Files.createDirectories(localDir);
        Files.createDirectories(depDir);
        Files.writeString(localDir.resolve("Child.ipspolicycmpttype"),
                "<?xml version=\"1.0\"?><PolicyCmptType supertype=\"DepA\"/>");
        Files.writeString(depDir.resolve("DepA.ipspolicycmpttype"),
                "<?xml version=\"1.0\"?><PolicyCmptType supertype=\"DepB\"/>");
        Files.writeString(depDir.resolve("DepB.ipspolicycmpttype"),
                "<?xml version=\"1.0\"?><PolicyCmptType/>");
        Files.writeString(depDir.resolve("Unrelated.ipspolicycmpttype"),
                "<?xml version=\"1.0\"?><PolicyCmptType/>");
        var options = new Ips2PlantOptions();

        // when
        var result = generator.generate(List.of(localDir), List.of(depDir), options, msg -> {});

        // then: DepA and DepB both included transitively, Unrelated excluded
        assertThat(result).contains("Child").contains("DepA").contains("DepB")
                .doesNotContain("Unrelated");
    }

    @Test
    void generate_dependencyFiltering_noLocalFiles_includesAllDependencies() throws IOException {
        // given: no local files, only dependency files
        var localDir = tempDir.resolve("local");
        var depDir = tempDir.resolve("dep");
        Files.createDirectories(localDir);
        Files.createDirectories(depDir);
        Files.writeString(depDir.resolve("A.ipspolicycmpttype"),
                "<?xml version=\"1.0\"?><PolicyCmptType/>");
        Files.writeString(depDir.resolve("B.ipspolicycmpttype"),
                "<?xml version=\"1.0\"?><PolicyCmptType/>");
        var options = new Ips2PlantOptions();

        // when
        var result = generator.generate(List.of(localDir), List.of(depDir), options, msg -> {});

        // then: all dependency files included unfiltered
        assertThat(result).contains("A").contains("B");
    }
}