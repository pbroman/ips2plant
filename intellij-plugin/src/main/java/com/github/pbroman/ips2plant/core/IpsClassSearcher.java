package com.github.pbroman.ips2plant.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class IpsClassSearcher {

    private final IpsFileCollector collector = new IpsFileCollector();

    /**
     * Search for IPS classes matching a wildcard pattern.
     * The pattern supports '*' as a wildcard matching any sequence of characters.
     * Examples: "Contract", "*Contract*", "com.example.*", "*Policy*Type"
     *
     * @param searchPattern wildcard pattern
     * @param modelDirs     directories to search in
     * @return map of matching fully qualified class name to file
     */
    public Map<String, File> search(String searchPattern, List<Path> modelDirs) {
        if (searchPattern == null || searchPattern.isBlank() || modelDirs.isEmpty()) {
            return Map.of();
        }

        var allFiles = collector.collect(modelDirs);
        var regex = wildcardToRegex(searchPattern);
        var pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

        var result = new LinkedHashMap<String, File>();
        for (var entry : allFiles.entrySet()) {
            var fqn = entry.getKey();
            var simpleName = fqn.contains(".") ? fqn.substring(fqn.lastIndexOf('.') + 1) : fqn;
            if (pattern.matcher(simpleName).matches()) {
                result.put(fqn, entry.getValue());
            }
        }
        return result;
    }

    /**
     * Transitively adds all supertypes of the given classes from the available model dirs.
     */
    public Map<String, File> addSupertypes(Map<String, File> baseFiles, List<Path> modelDirs) {
        var allFiles = collector.collect(modelDirs);
        var result = new LinkedHashMap<>(baseFiles);

        var toProcess = new LinkedHashMap<>(baseFiles);
        while (!toProcess.isEmpty()) {
            var newlyFound = new LinkedHashMap<String, File>();
            for (var file : toProcess.values()) {
                var supertype = extractSupertype(file);
                if (supertype != null && !result.containsKey(supertype) && allFiles.containsKey(supertype)) {
                    newlyFound.put(supertype, allFiles.get(supertype));
                }
            }
            if (newlyFound.isEmpty()) break;
            result.putAll(newlyFound);
            toProcess = newlyFound;
        }
        return result;
    }

    /**
     * Adds all classes that reference any of the given classes through associations.
     */
    public Map<String, File> addReferencingClasses(Map<String, File> baseFiles, List<Path> modelDirs) {
        var allFiles = collector.collect(modelDirs);
        var targetFqns = baseFiles.keySet();

        var result = new LinkedHashMap<>(baseFiles);
        for (var entry : allFiles.entrySet()) {
            if (result.containsKey(entry.getKey())) continue;
            var targets = extractAssociationTargets(entry.getValue());
            for (var target : targets) {
                if (targetFqns.contains(target)) {
                    result.put(entry.getKey(), entry.getValue());
                    break;
                }
            }
        }
        return result;
    }

    private String extractSupertype(File file) {
        try {
            var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
            var root = doc.getDocumentElement();
            var supertype = root.getAttribute("supertype");
            if (supertype != null && !supertype.isBlank()) return supertype;
            var superEnum = root.getAttribute("superEnumType");
            if (superEnum != null && !superEnum.isBlank()) return superEnum;
        } catch (IOException | SAXException | ParserConfigurationException e) {
            // skip unparseable files
        }
        return null;
    }

    private Set<String> extractAssociationTargets(File file) {
        var targets = new HashSet<String>();
        try {
            var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
            var root = doc.getDocumentElement();
            collectAttrValues(targets, root.getElementsByTagName("Association"), "target");
        } catch (IOException | SAXException | ParserConfigurationException e) {
            // skip unparseable files
        }
        return targets;
    }

    private void collectAttrValues(Set<String> set, NodeList nodes, String attrName) {
        for (int i = 0; i < nodes.getLength(); i++) {
            var element = (Element) nodes.item(i);
            var value = element.getAttribute(attrName);
            if (value != null && !value.isBlank()) {
                set.add(value);
            }
        }
    }

    static String wildcardToRegex(String wildcard) {
        var sb = new StringBuilder();
        for (int i = 0; i < wildcard.length(); i++) {
            char c = wildcard.charAt(i);
            if (c == '*') {
                sb.append(".*");
            } else if (".()[]{}+^$|\\".indexOf(c) >= 0) {
                sb.append('\\').append(c);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
