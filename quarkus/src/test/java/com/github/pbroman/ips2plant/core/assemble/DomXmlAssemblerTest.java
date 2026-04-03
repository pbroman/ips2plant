package com.github.pbroman.ips2plant.core.assemble;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class DomXmlAssemblerTest {

    private final DomXmlAssembler assembler = new DomXmlAssembler();

    @TempDir
    Path tempDir;

    @Test
    void assemble_singleFile_producesCollectionWithClassName() throws IOException {
        // given
        var file = writeXmlFile("A.ipspolicycmpttype", "<PolicyCmptType supertype=\"Base\"/>");
        var destination = tempDir.resolve("collection.xml");

        // when
        assembler.assemble(Map.of("com.example.A", file), destination);

        // then
        var xml = Files.readString(destination);
        assertThat(xml).contains("<collection>")
                .contains("className=\"com.example.A\"")
                .contains("supertype=\"Base\"");
    }

    @Test
    void assemble_removesNamespaceAttributes() throws IOException {
        // given
        var file = writeXmlFile("B.ipspolicycmpttype",
                "<PolicyCmptType xmlns=\"http://www.faktorzehn.org\" "
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "xsi:schemaLocation=\"http://www.faktorzehn.org schema.xsd\"/>");
        var destination = tempDir.resolve("collection.xml");

        // when
        assembler.assemble(Map.of("B", file), destination);

        // then
        var xml = Files.readString(destination);
        assertThat(xml).doesNotContain("xmlns=")
                .doesNotContain("xmlns:xsi=")
                .doesNotContain("xsi:schemaLocation=");
    }

    @Test
    void assemble_multipleFiles_allIncludedInCollection() throws IOException {
        // given
        var fileA = writeXmlFile("A.ipspolicycmpttype", "<PolicyCmptType/>");
        var fileB = writeXmlFile("B.ipsproductcmpttype", "<ProductCmptType2/>");
        var files = new LinkedHashMap<String, File>();
        files.put("com.A", fileA);
        files.put("com.B", fileB);
        var destination = tempDir.resolve("collection.xml");

        // when
        assembler.assemble(files, destination);

        // then
        var xml = Files.readString(destination);
        assertThat(xml).contains("className=\"com.A\"")
                .contains("className=\"com.B\"");
    }

    @Test
    void assemble_preservesChildElements() throws IOException {
        // given
        var file = writeXmlFile("C.ipspolicycmpttype",
                "<PolicyCmptType><Attribute name=\"premium\"/></PolicyCmptType>");
        var destination = tempDir.resolve("collection.xml");

        // when
        assembler.assemble(Map.of("C", file), destination);

        // then
        var xml = Files.readString(destination);
        assertThat(xml).contains("Attribute")
                .contains("name=\"premium\"");
    }

    @Test
    void assemble_withMavenModules_addsMavenModuleAttribute() throws IOException {
        // given
        var file = writeXmlFile("D.ipspolicycmpttype", "<PolicyCmptType/>");
        var destination = tempDir.resolve("collection.xml");
        var mavenModules = Map.of("com.D", "com.example:my-module");

        // when
        assembler.assemble(Map.of("com.D", file), destination, mavenModules);

        // then
        var xml = Files.readString(destination);
        assertThat(xml).contains("mavenModule=\"com.example:my-module\"");
    }

    @Test
    void assemble_withMavenModules_noMatchingModule_noMavenModuleAttribute() throws IOException {
        // given
        var file = writeXmlFile("E.ipspolicycmpttype", "<PolicyCmptType/>");
        var destination = tempDir.resolve("collection.xml");
        var mavenModules = Map.of("com.Other", "com.example:other-module");

        // when
        assembler.assemble(Map.of("com.E", file), destination, mavenModules);

        // then
        var xml = Files.readString(destination);
        assertThat(xml).doesNotContain("mavenModule=");
    }

    @Test
    void assemble_withoutMavenModules_noMavenModuleAttribute() throws IOException {
        // given
        var file = writeXmlFile("F.ipspolicycmpttype", "<PolicyCmptType/>");
        var destination = tempDir.resolve("collection.xml");

        // when
        assembler.assemble(Map.of("com.F", file), destination);

        // then
        var xml = Files.readString(destination);
        assertThat(xml).doesNotContain("mavenModule=");
    }

    private File writeXmlFile(String name, String content) throws IOException {
        var file = tempDir.resolve(name);
        Files.writeString(file, "<?xml version=\"1.0\"?>\n" + content);
        return file.toFile();
    }
}