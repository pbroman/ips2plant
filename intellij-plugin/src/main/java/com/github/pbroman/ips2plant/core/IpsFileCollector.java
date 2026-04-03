package com.github.pbroman.ips2plant.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class IpsFileCollector {

    static final List<String> SUPPORTED_EXTENSIONS = List.of(
            ".ipspolicycmpttype",
            ".ipsproductcmpttype",
            ".ipsenumtype",
            ".ipsenumcontent",
            ".ipstablestructure"
    );

    static boolean isSupportedIpsFile(String fileName) {
        for (var ext : SUPPORTED_EXTENSIONS) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    public Map<String, File> collect(List<Path> dirPaths) {
        var result = new HashMap<String, File>();
        dirPaths.forEach(path -> {
            var dir = path.toFile();
            recursiveCollect(dir, dir, result);
        });
        return result;
    }

    public Set<String> extractReferencedTypes(Map<String, File> ipsFiles) {
        var referenced = new HashSet<String>();
        try {
            var domBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            for (var file : ipsFiles.values()) {
                try {
                    var doc = domBuilder.parse(file);
                    var root = doc.getDocumentElement();
                    addIfPresent(referenced, root.getAttribute("supertype"));
                    addIfPresent(referenced, root.getAttribute("productCmptType"));
                    collectAttributeValues(referenced, root.getElementsByTagName("Association"), "target");
                    collectAttributeValues(referenced, root.getElementsByTagName("Attribute"), "datatype");
                    collectAttributeValues(referenced, root.getElementsByTagName("TableStructure"), "tableStructure");
                } catch (IOException | SAXException e) {
                    // skip unparseable files
                }
            }
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        return referenced;
    }

    private void collectAttributeValues(Set<String> set, NodeList nodes, String attrName) {
        for (int i = 0; i < nodes.getLength(); i++) {
            var element = (Element) nodes.item(i);
            addIfPresent(set, element.getAttribute(attrName));
        }
    }

    private void addIfPresent(Set<String> set, String value) {
        if (value != null && !value.isBlank()) {
            set.add(value);
        }
    }

    private void recursiveCollect(File file, File root, Map<String, File> map) {
        if (file.isFile() && isSupportedIpsFile(file.getName())) {
            var relativePath = root.toPath().relativize(file.toPath()).toString();
            var className = relativePath
                    .substring(0, relativePath.lastIndexOf('.'))
                    .replace(File.separatorChar, '.');
            if (file.getName().endsWith(".ipsenumcontent")) {
                className += "Content";
            }
            map.put(className, file);
        } else if (file.isDirectory()) {
            for (File child : Objects.requireNonNull(file.listFiles())) {
                recursiveCollect(child, root, map);
            }
        }
    }
}