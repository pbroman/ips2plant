package com.github.pbroman.ips2plant.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.pbroman.ips2plant.core.assemble.DomXmlAssembler;
import com.github.pbroman.ips2plant.core.collect.DefaultIpsFileCollector;
import com.github.pbroman.ips2plant.Ips2PlantRunner;
import com.github.pbroman.ips2plant.core.xslt.DefaultXsltProcessor;

public class Ips2PlantRunnerTest extends AbstractTest {

    Ips2PlantRunner ips2PlantRunner;

    @BeforeEach
    void setup() {
        var collector = new DefaultIpsFileCollector();
        var assembler = new DomXmlAssembler();
        var xsltProcessor = new DefaultXsltProcessor();
        ips2PlantRunner = new Ips2PlantRunner(collector, assembler, xsltProcessor);
    }

    // TODO
    @Test
    void test() throws Exception {

    }
}
