package com.github.pbroman.ips2plant.runner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.pbroman.ips2plant.AbstractTest;
import com.github.pbroman.ips2plant.assemble.DomXmlAssembler;
import com.github.pbroman.ips2plant.collect.DefaultIpsFileCollector;
import com.github.pbroman.ips2plant.xslt.SaxonXsltProcessor;

public class Ips2PlantRunnerTest extends AbstractTest {

    Ips2PlantRunner ips2PlantRunner;

    @BeforeEach
    void setup() {
        var collector = new DefaultIpsFileCollector();
        var assembler = new DomXmlAssembler();
        var xsltProcessor = new SaxonXsltProcessor();
        ips2PlantRunner = new Ips2PlantRunner(collector, assembler, xsltProcessor);
    }

    // TODO
    @Test
    void test() throws Exception {

    }
}
