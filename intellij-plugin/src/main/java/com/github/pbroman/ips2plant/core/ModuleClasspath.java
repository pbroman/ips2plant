package com.github.pbroman.ips2plant.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Model dirs visible from a root module: its own dirs plus those of its compile/provided-scope Maven
 * dependency closure (local modules via their poms, resolved jars by artifactId). No IntelliJ dependencies.
 */
public final class ModuleClasspath {

    private static final Set<String> CLASSPATH_SCOPES = Set.of("", "compile", "provided");

    private ModuleClasspath() {
    }

    /**
     * @param rootModelDir      model dir containing the root class
     * @param localModelDirs    all local model dirs of the project
     * @param dependencyModules extracted dependency model dir to "groupId:artifactId" (or plain artifactId)
     * @return visible model dirs, empty if the root module has no pom.xml (classpath unknown)
     */
    public static Optional<Set<Path>> visibleDirs(Path rootModelDir, Collection<Path> localModelDirs,
                                                  Map<Path, String> dependencyModules) {
        var rootPomDir = findPomDir(rootModelDir);
        if (rootPomDir == null) {
            return Optional.empty();
        }
        var localByPomDir = new HashMap<Path, List<Path>>();
        var pomDirByArtifact = new HashMap<String, Path>();
        for (var dir : localModelDirs) {
            var pomDir = findPomDir(dir);
            if (pomDir == null) {
                continue;
            }
            localByPomDir.computeIfAbsent(pomDir, k -> new ArrayList<>()).add(dir);
            readArtifactId(pomDir.resolve("pom.xml")).ifPresent(id -> pomDirByArtifact.putIfAbsent(id, pomDir));
        }
        var jarDirByArtifact = new HashMap<String, Path>();
        dependencyModules.forEach((dir, module) -> {
            if (module != null) {
                jarDirByArtifact.putIfAbsent(module.substring(module.lastIndexOf(':') + 1), dir);
            }
        });

        var visible = new LinkedHashSet<Path>();
        var visitedPomDirs = new HashSet<Path>();
        var queue = new ArrayDeque<Path>();
        queue.add(rootPomDir);
        visitedPomDirs.add(rootPomDir);
        while (!queue.isEmpty()) {
            var pomDir = queue.poll();
            visible.addAll(localByPomDir.getOrDefault(pomDir, List.of()));
            for (var artifactId : declaredClasspathDependencies(pomDir)) {
                var localPomDir = pomDirByArtifact.get(artifactId);
                if (localPomDir != null && visitedPomDirs.add(localPomDir)) {
                    queue.add(localPomDir);
                }
                var jarDir = jarDirByArtifact.get(artifactId);
                if (jarDir != null) {
                    visible.add(jarDir);
                }
            }
        }
        visible.add(rootModelDir);
        return Optional.of(visible);
    }

    private static Path findPomDir(Path modelDir) {
        for (var current = modelDir.toAbsolutePath().getParent(); current != null; current = current.getParent()) {
            if (Files.isRegularFile(current.resolve("pom.xml"))) {
                return current;
            }
        }
        return null;
    }

    private static Optional<String> readArtifactId(Path pom) {
        return parse(pom).map(root -> childText(root, "artifactId")).filter(s -> !s.isBlank());
    }

    /** Declared dependencies of the pom and of its parent chain (via relativePath, default ../pom.xml). */
    private static Set<String> declaredClasspathDependencies(Path pomDir) {
        var result = new LinkedHashSet<String>();
        var seen = new HashSet<Path>();
        var pom = pomDir.resolve("pom.xml");
        while (pom != null && Files.isRegularFile(pom) && seen.add(pom.toAbsolutePath().normalize())) {
            var root = parse(pom).orElse(null);
            if (root == null) {
                break;
            }
            for (var dependencies : childElements(root, "dependencies")) {
                for (var dependency : childElements(dependencies, "dependency")) {
                    var scope = childText(dependency, "scope");
                    var artifactId = childText(dependency, "artifactId");
                    if (CLASSPATH_SCOPES.contains(scope) && !artifactId.isBlank() && !artifactId.contains("${")) {
                        result.add(artifactId);
                    }
                }
            }
            pom = parentPom(pom, root);
        }
        return result;
    }

    private static Path parentPom(Path pom, Element root) {
        var parents = childElements(root, "parent");
        if (parents.isEmpty()) {
            return null;
        }
        var relative = childText(parents.get(0), "relativePath");
        var target = pom.getParent().resolve(relative.isBlank() ? "../pom.xml" : relative).normalize();
        return Files.isDirectory(target) ? target.resolve("pom.xml") : target;
    }

    private static Optional<Element> parse(Path pom) {
        try {
            return Optional.of(DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(pom.toFile()).getDocumentElement());
        } catch (IOException | SAXException | ParserConfigurationException e) {
            return Optional.empty();
        }
    }

    private static List<Element> childElements(Element parent, String name) {
        var result = new ArrayList<Element>();
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && e.getTagName().equals(name)) {
                result.add(e);
            }
        }
        return result;
    }

    private static String childText(Element parent, String name) {
        var children = childElements(parent, name);
        return children.isEmpty() ? "" : children.get(0).getTextContent().trim();
    }
}
