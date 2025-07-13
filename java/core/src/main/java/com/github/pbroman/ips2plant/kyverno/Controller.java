package com.github.pbroman.ips2plant.kyverno;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.DefaultResourceLoader;

import com.github.pbroman.ips2plant.api.IpsFileCollector;
import com.github.pbroman.ips2plant.api.XmlAssembler;
import com.github.pbroman.ips2plant.api.XsltProcessor;


public class Controller {

    public static final String IPS_TO_PLANT_PATH = "classpath:ips2plant.xsl";

    private final IpsFileCollector collector;
    private final  XmlAssembler assembler;
    private final  XsltProcessor xsltProcessor;

    public Controller(IpsFileCollector collector, XmlAssembler assembler, XsltProcessor xsltProcessor) {
        this.collector = collector;
        this.assembler = assembler;
        this.xsltProcessor = xsltProcessor;
    }

    public void execute(List<Path> modelBasePaths, Path destination) throws IOException {
        var ipsFiles = collector.collect(modelBasePaths);

        // TODO workdir
        var collection = Files.createTempFile("ipsCollection", "xml").toAbsolutePath();
        assembler.assemble(ipsFiles, collection);

        var xslt = new DefaultResourceLoader().getResource(IPS_TO_PLANT_PATH).getFile().toPath();

        xsltProcessor.process(xslt, collection, destination, Map.of());

        Files.delete(collection);
    }
}
