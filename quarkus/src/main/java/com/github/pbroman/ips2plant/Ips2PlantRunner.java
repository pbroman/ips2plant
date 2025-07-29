package com.github.pbroman.ips2plant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.pbroman.ips2plant.core.api.IpsFileCollector;
import com.github.pbroman.ips2plant.core.api.XmlAssembler;
import com.github.pbroman.ips2plant.core.api.XsltProcessor;

public class Ips2PlantRunner {

    private static final Logger log = LoggerFactory.getLogger(Ips2PlantRunner.class);

    public static final String IPS_TO_PLANT_PATH = "/xsl/ips2plant.xsl";
    public static final String COLLECTION_FILENAME = "collection.xml";

    private final IpsFileCollector collector;
    private final  XmlAssembler assembler;
    private final  XsltProcessor xsltProcessor;

    public Ips2PlantRunner(IpsFileCollector collector, XmlAssembler assembler, XsltProcessor xsltProcessor) {
        this.collector = collector;
        this.assembler = assembler;
        this.xsltProcessor = xsltProcessor;
    }

    public void execute(List<Path> modelDirPaths, Map<String, String> stringParams, Path output, Path workdir) throws IOException  {

        var modelPathsGiven = !Objects.isNull(modelDirPaths) && !modelDirPaths.isEmpty();
        var workdirGiven = !Objects.isNull(workdir);
        if (!modelPathsGiven && !workdirGiven) {
            throw new IllegalStateException("Either a workdir or path(s) to model directories is required.");
        }
        if (!modelPathsGiven && !(new File(workdir.toFile(), COLLECTION_FILENAME).exists())) {
            throw new IllegalStateException("When no model paths are given, the workdir must contain a " + COLLECTION_FILENAME);
        }
        if (Objects.isNull(output)) {
            throw new IllegalStateException("An output file is required, received null.");
        }

        Path collection;
        if (workdirGiven) {
            if (modelPathsGiven) {
                log.info("Creating/overriding xml file for the ips file assembly: {}/{}", workdir, COLLECTION_FILENAME);
            } else {
                log.info("Using already present ips file collection: {}/{}", workdir, COLLECTION_FILENAME);
            }
            collection = new File(workdir.toFile(), COLLECTION_FILENAME).toPath();
        } else {
            log.info("Creating temp xml file for the ips file assembly");
            collection = Files.createTempFile("tempCollection", "xml").toAbsolutePath();
        }

        if (modelPathsGiven) {
            log.info("Assembling ips models...");
            var ipsFiles = collector.collect(modelDirPaths);
            assembler.assemble(ipsFiles, collection);
        }

        // Packed in an uber-jar, the resource will be zipped, so we have to copy it to a temp file
        var xsltStream = Objects.requireNonNull(Ips2PlantRunner.class.getResourceAsStream(IPS_TO_PLANT_PATH));
        var tempXsl = File.createTempFile("com/github/pbroman/ips2plant", "xsl");
        try (var out = new FileOutputStream(tempXsl)) {
            IOUtils.copy(xsltStream, out);
        }

        log.info("Xslt processing...");
        xsltProcessor.process(tempXsl.toPath(), collection, output, stringParams);
        log.info("Finished processing, result in {}", output.toAbsolutePath());

        if (!workdirGiven) {
            Files.delete(collection);
        }
        tempXsl.delete();
    }
}
