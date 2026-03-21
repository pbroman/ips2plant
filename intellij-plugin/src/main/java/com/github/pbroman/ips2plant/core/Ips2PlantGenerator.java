package com.github.pbroman.ips2plant.core;

import java.nio.file.Path;
import java.util.List;

public class Ips2PlantGenerator {

    private final IpsFileCollector collector = new IpsFileCollector();
    private final XmlAssembler assembler = new XmlAssembler();
    private final XsltProcessor xsltProcessor = new XsltProcessor();

    public String generate(List<Path> modelDirPaths, Ips2PlantOptions options) {
        var ipsFiles = collector.collect(modelDirPaths);
        if (ipsFiles.isEmpty()) {
            return "@startuml\nnote \"No IPS model files found\" as N1\n@enduml";
        }
        var collectionXml = assembler.assemble(ipsFiles);
        return xsltProcessor.transform(collectionXml, options.toXsltParams());
    }
}