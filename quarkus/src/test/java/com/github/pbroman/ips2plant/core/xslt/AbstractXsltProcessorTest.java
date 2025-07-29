package com.github.pbroman.ips2plant.core.xslt;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.pbroman.ips2plant.core.AbstractTest;
import com.github.pbroman.ips2plant.core.api.XsltProcessor;

public abstract class AbstractXsltProcessorTest extends AbstractTest {

    @TempDir
    Path tempDir;

    protected XsltProcessor processor;

    protected Path resultFile;
    protected File xsl;
    protected File xml;

    @BeforeEach
    void commonSetUp() throws Exception {
        resultFile = tempDir.resolve("testResult.txt");
        xsl = getResourceFile("processor/test.xsl");
        xml = getResourceFile("processor/test.xml");
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
