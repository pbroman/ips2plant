package com.github.pbroman.ips2plant.core;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class Ips2PlantGenerator {

    private final IpsFileCollector collector = new IpsFileCollector();
    private final XmlAssembler assembler = new XmlAssembler();
    private final XsltProcessor xsltProcessor = new XsltProcessor();

    public String generate(List<Path> modelDirPaths, Ips2PlantOptions options) {
        return generate(modelDirPaths, options, msg -> {});
    }

    public String generate(List<Path> modelDirPaths, Ips2PlantOptions options, Consumer<String> statusCallback) {
        return generate(modelDirPaths, List.of(), options, statusCallback);
    }

    public String generate(Map<String, File> ipsFiles, Ips2PlantOptions options, Consumer<String> statusCallback) {
        return generate(ipsFiles, options, statusCallback, Map.of());
    }

    public String generate(Map<String, File> ipsFiles, Ips2PlantOptions options, Consumer<String> statusCallback, Map<String, String> mavenModules) {
        if (ipsFiles.isEmpty()) {
            return "@startuml\nnote \"No IPS model files found\" as N1\n@enduml";
        }

        var collectionXml = assembler.assemble(ipsFiles, mavenModules);
        statusCallback.accept("Transforming to PlantUML...");
        return xsltProcessor.transform(collectionXml, options.toXsltParams());
    }

    public String generate(List<Path> localDirs, List<Path> dependencyDirs, Ips2PlantOptions options, Consumer<String> statusCallback) {
        return generate(localDirs, dependencyDirs, options, statusCallback, Map.of());
    }

    public String generate(List<Path> localDirs, List<Path> dependencyDirs, Ips2PlantOptions options,
                           Consumer<String> statusCallback, Map<Path, String> dependencyModules) {
        statusCallback.accept("Collecting IPS files...");
        var ipsFiles = collector.collect(localDirs);

        if (!dependencyDirs.isEmpty()) {
            statusCallback.accept("Filtering dependency models...");
            var depFiles = collector.collect(dependencyDirs);
            if (ipsFiles.isEmpty()) {
                ipsFiles.putAll(depFiles);
            } else {
                var included = new HashMap<String, File>();
                var referencedTypes = collector.extractReferencedTypes(ipsFiles);
                while (true) {
                    var newlyIncluded = filterByReferencedTypes(depFiles, referencedTypes);
                    newlyIncluded.keySet().removeAll(included.keySet());
                    if (newlyIncluded.isEmpty()) break;
                    included.putAll(newlyIncluded);
                    referencedTypes = collector.extractReferencedTypes(newlyIncluded);
                }
                ipsFiles.putAll(included);
            }
        }

        Map<String, String> mavenModules = Map.of();
        if (options.isShowMavenModule()) {
            var resolver = new MavenModuleResolver();
            for (var entry : dependencyModules.entrySet()) {
                resolver.registerModule(entry.getKey(), entry.getValue());
            }
            mavenModules = resolver.resolveAll(ipsFiles);
        }

        return generate(ipsFiles, options, statusCallback, mavenModules);
    }

    private Map<String, File> filterByReferencedTypes(Map<String, File> depFiles, Set<String> referencedTypes) {
        var filtered = new HashMap<String, File>();
        for (var entry : depFiles.entrySet()) {
            if (referencedTypes.contains(entry.getKey())) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }
}