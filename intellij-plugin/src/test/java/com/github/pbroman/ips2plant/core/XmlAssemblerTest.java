package com.github.pbroman.ips2plant.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class XmlAssemblerTest {

    private final XmlAssembler assembler = new XmlAssembler();

    @TempDir
    Path tempDir;

    @Test
    void assemble_singleFile_producesCollectionWithClassName() throws IOException {
        // given
        var file = writeXmlFile("A.ipspolicycmpttype",
                "<PolicyCmptType supertype=\"Base\"/>");

        // when
        var xml = assembler.assemble(Map.of("com.example.A", file));

        // then
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

        // when
        var xml = assembler.assemble(Map.of("B", file));

        // then
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

        // when
        var xml = assembler.assemble(files);

        // then
        assertThat(xml).contains("className=\"com.A\"")
                .contains("className=\"com.B\"");
    }

    @Test
    void assemble_preservesChildElements() throws IOException {
        // given
        var file = writeXmlFile("C.ipspolicycmpttype",
                "<PolicyCmptType><Attribute name=\"premium\"/></PolicyCmptType>");

        // when
        var xml = assembler.assemble(Map.of("C", file));

        // then
        assertThat(xml).contains("Attribute")
                .contains("name=\"premium\"");
    }

    private File writeXmlFile(String name, String content) throws IOException {
        var file = tempDir.resolve(name);
        Files.writeString(file, "<?xml version=\"1.0\"?>\n" + content);
        return file.toFile();
    }
}