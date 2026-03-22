package com.github.pbroman.ips2plant.core;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class IpsProjectDetector {

    private static final String IPS_PROJECT_FILE = ".ipsproject";

    public List<Path> detectModelDirs(Path projectRoot) {
        var ipsProjectFiles = findIpsProjectFiles(projectRoot);
        var modelDirs = new ArrayList<Path>();
        for (var ipsProjectFile : ipsProjectFiles) {
            modelDirs.addAll(parseModelDirs(ipsProjectFile));
        }
        return modelDirs;
    }

    private List<Path> findIpsProjectFiles(Path root) {
        try (Stream<Path> walk = Files.walk(root, 20)) {
            return walk
                    .filter(p -> p.getFileName().toString().equals(IPS_PROJECT_FILE))
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private boolean containsIpsFiles(Path dir) {
        var found = new boolean[]{false};
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.getFileName().toString().matches(".+\\.ips.+")) {
                        found[0] = true;
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            // if we can't walk it, treat as empty
        }
        return found[0];
    }

    private List<Path> parseModelDirs(Path ipsProjectFile) {
        var result = new ArrayList<Path>();
        try {
            var doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(ipsProjectFile.toFile());

            NodeList entries = doc.getElementsByTagName("Entry");
            for (int i = 0; i < entries.getLength(); i++) {
                var entry = (Element) entries.item(i);
                if ("src".equals(entry.getAttribute("type"))) {
                    var sourceFolder = entry.getAttribute("sourceFolder");
                    if (!sourceFolder.isBlank()) {
                        var modelDir = ipsProjectFile.getParent().resolve(sourceFolder);
                        if (Files.isDirectory(modelDir) && containsIpsFiles(modelDir)) {
                            result.add(modelDir);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // skip unparseable .ipsproject files
        }
        return result;
    }
}