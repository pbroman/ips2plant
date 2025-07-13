package com.github.pbroman.ips2plant.kyverno;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.io.IOUtils;

import com.github.pbroman.ips2plant.api.IpsFileCollector;
import com.github.pbroman.ips2plant.api.XmlAssembler;
import com.github.pbroman.ips2plant.api.XsltProcessor;


public class Ips2PlantRunner {

    public static final String IPS_TO_PLANT_PATH = "/ips2plant.xsl";

    private final IpsFileCollector collector;
    private final  XmlAssembler assembler;
    private final  XsltProcessor xsltProcessor;

    public Ips2PlantRunner(IpsFileCollector collector, XmlAssembler assembler, XsltProcessor xsltProcessor) {
        this.collector = collector;
        this.assembler = assembler;
        this.xsltProcessor = xsltProcessor;
    }

    public void execute(List<Path> modelDirPaths, Map<String, String> stringParams, Path pumlResult) throws IOException, URISyntaxException {
        if (Objects.isNull(modelDirPaths) || modelDirPaths.isEmpty()) {
            throw new IllegalStateException("There are no paths to model directories. At least one is required.");
        }
        if (Objects.isNull(pumlResult)) {
            throw new IllegalStateException("A result file is required, received null.");
        }

        var ipsFiles = collector.collect(modelDirPaths);

        var collection = Files.createTempFile("ipsCollection", "xml").toAbsolutePath();
        assembler.assemble(ipsFiles, collection);

        var xsltStream = Objects.requireNonNull(Ips2PlantRunner.class.getResourceAsStream(IPS_TO_PLANT_PATH));
        var tempXsl = File.createTempFile("ips2plant", "xsl");
        try (var out = new FileOutputStream(tempXsl)) {
            IOUtils.copy(xsltStream, out);
        }

        xsltProcessor.process(tempXsl.toPath(), collection, pumlResult, stringParams);

        Files.delete(collection);
        tempXsl.delete();
    }
}
