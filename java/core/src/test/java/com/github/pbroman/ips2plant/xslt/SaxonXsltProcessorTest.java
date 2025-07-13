package com.github.pbroman.ips2plant.xslt;


import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;

import com.github.pbroman.ips2plant.api.XsltProcessor;

public class SaxonXsltProcessorTest {

    @TempDir
    Path tempDir;

    XsltProcessor processor = new SaxonXsltProcessor();
    Path resultFile;
    File xsl;
    File xml;

    @BeforeEach
    void setUp() throws Exception {
        resultFile = tempDir.resolve("testResult.txt");
        xsl = new ClassPathResource("processor/test.xsl").getFile();
        xml = new ClassPathResource("processor/test.xml").getFile();
    }

    @Test
    void xslProcessor_basicFunctionality() throws Exception{
        // when
        processor.process(xsl.toPath(), xml.toPath(), resultFile, Map.of());

        // then
        var result = Files.readAllLines(resultFile);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).trim()).isEqualTo("baa");
    }

    @Test
    void xslProcessor_plagParam() throws Exception{
        // when
        processor.process(xsl.toPath(), xml.toPath(), resultFile, Map.of("flag", "false"));

        // then
        var result = Files.readAllLines(resultFile);
        assertThat(result).hasSize(0);
    }

}
