package com.github.pbroman.ips2plant.kyverno;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.pbroman.ips2plant.assemble.DomXmlAssembler;
import com.github.pbroman.ips2plant.collect.DefaultIpsFileCollector;
import com.github.pbroman.ips2plant.xslt.SaxonXsltProcessor;

public class ControllerTest {

    Controller controller;

    @BeforeEach
    void setup() {
        var collector = new DefaultIpsFileCollector();
        var assembler = new DomXmlAssembler();
        var xsltProcessor = new SaxonXsltProcessor();
        controller = new Controller(collector, assembler, xsltProcessor);
    }

    @Test
    void test() throws Exception {

        var ipsPath = Path.of("some/path");
        controller.execute(List.of(ipsPath), new File("myPlant.puml").toPath());
    }

    /*
        TODO
        * Unit tests
        * CLI setup
        * Wrap CLI in bash script
     */

}
