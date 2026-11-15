package com.github.pbroman.ips2plant.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Writes copies of policy type files that additionally contain the attributes and associations of their whole
 * supertype chain, each marked with {@code inherited="true"}. No IntelliJ dependencies.
 */
public class IpsSupertypeMerger {

    static final String INHERITED = "inherited";
    private static final String POLICY_EXTENSION = ".ipspolicycmpttype";

    /**
     * @param files    classes to merge (fully qualified name to file); non-policy types are passed through
     * @param allFiles every known class, used to find the supertypes
     * @param tempDir  directory the merged copies are written to
     * @return the same keys, pointing at the merged copies
     */
    public Map<String, File> merge(Map<String, File> files, Map<String, File> allFiles, Path tempDir) throws IOException {
        var result = new LinkedHashMap<String, File>();
        var builder = newBuilder();
        var concreteSubtypes = concreteSubtypesByAncestor(files, allFiles, builder);
        for (var entry : files.entrySet()) {
            var file = entry.getValue();
            if (!file.getName().endsWith(POLICY_EXTENSION)) {
                result.put(entry.getKey(), file);
                continue;
            }
            try {
                var doc = builder.parse(file);
                mergeInto(doc.getDocumentElement(), allFiles, builder);
                retarget(doc.getDocumentElement(), files, allFiles, builder, concreteSubtypes);
                var target = tempDir.resolve(entry.getKey().replace('.', '_') + POLICY_EXTENSION);
                write(doc, target);
                result.put(entry.getKey(), target.toFile());
            } catch (SAXException | TransformerException e) {
                result.put(entry.getKey(), file);
            }
        }
        return result;
    }

    private void mergeInto(Element root, Map<String, File> allFiles, javax.xml.parsers.DocumentBuilder builder)
            throws IOException, SAXException {
        var attributeNames = new HashSet<String>();
        var associationKeys = new HashSet<String>();
        collectKeys(root, attributeNames, associationKeys);

        var visited = new HashSet<String>();
        var superName = root.getAttribute("supertype");
        while (!superName.isBlank() && visited.add(superName)) {
            var superFile = allFiles.get(superName);
            if (superFile == null) {
                break;
            }
            var superRoot = builder.parse(superFile).getDocumentElement();
            for (Node n = superRoot.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (!(n instanceof Element e)) {
                    continue;
                }
                boolean add = switch (e.getTagName()) {
                    case "Attribute" -> attributeNames.add(e.getAttribute("name"));
                    case "Association" -> associationKeys.add(associationKey(e));
                    default -> false;
                };
                if (add) {
                    var copy = (Element) root.getOwnerDocument().importNode(e, true);
                    copy.setAttribute(INHERITED, "true");
                    root.appendChild(copy);
                }
            }
            superName = superRoot.getAttribute("supertype");
        }
    }

    /** For every supertype of a selected concrete policy type: the selected concrete types below it. */
    private Map<String, Set<String>> concreteSubtypesByAncestor(Map<String, File> files, Map<String, File> allFiles,
                                                                javax.xml.parsers.DocumentBuilder builder) {
        var result = new HashMap<String, Set<String>>();
        for (var entry : files.entrySet()) {
            if (!entry.getValue().getName().endsWith(POLICY_EXTENSION)
                    || isAbstract(entry.getKey(), files, allFiles, builder)) {
                continue;
            }
            var visited = new HashSet<String>();
            var superName = supertypeOf(entry.getKey(), files, allFiles, builder);
            while (!superName.isBlank() && visited.add(superName)) {
                result.computeIfAbsent(superName, k -> new LinkedHashSet<>()).add(entry.getKey());
                superName = supertypeOf(superName, files, allFiles, builder);
            }
        }
        return result;
    }

    /**
     * An association to an abstract class that is not itself selected is replaced by one association per selected
     * concrete subtype, so no arrow points at a class that is not in the diagram.
     */
    private void retarget(Element root, Map<String, File> files, Map<String, File> allFiles,
                          javax.xml.parsers.DocumentBuilder builder, Map<String, Set<String>> concreteSubtypes) {
        for (Node n = root.getFirstChild(); n != null; ) {
            var next = n.getNextSibling();
            if (n instanceof Element e && e.getTagName().equals("Association")) {
                var target = e.getAttribute("target");
                var subs = concreteSubtypes.get(target);
                if (subs != null && !files.containsKey(target) && isAbstract(target, files, allFiles, builder)) {
                    for (var sub : subs) {
                        var copy = (Element) e.cloneNode(true);
                        copy.setAttribute("target", sub);
                        root.insertBefore(copy, e);
                    }
                    root.removeChild(e);
                }
            }
            n = next;
        }
    }

    private String supertypeOf(String fqn, Map<String, File> files, Map<String, File> allFiles,
                               javax.xml.parsers.DocumentBuilder builder) {
        var root = rootOf(fqn, files, allFiles, builder);
        return root == null ? "" : root.getAttribute("supertype");
    }

    private boolean isAbstract(String fqn, Map<String, File> files, Map<String, File> allFiles,
                               javax.xml.parsers.DocumentBuilder builder) {
        var root = rootOf(fqn, files, allFiles, builder);
        return root != null && "true".equals(root.getAttribute("abstract"));
    }

    private Element rootOf(String fqn, Map<String, File> files, Map<String, File> allFiles,
                           javax.xml.parsers.DocumentBuilder builder) {
        var file = allFiles.getOrDefault(fqn, files.get(fqn));
        if (file == null) {
            return null;
        }
        try {
            return builder.parse(file).getDocumentElement();
        } catch (IOException | SAXException e) {
            return null;
        }
    }

    private void collectKeys(Element root, Set<String> attributeNames, Set<String> associationKeys) {
        for (Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e) {
                if (e.getTagName().equals("Attribute")) {
                    attributeNames.add(e.getAttribute("name"));
                } else if (e.getTagName().equals("Association")) {
                    associationKeys.add(associationKey(e));
                }
            }
        }
    }

    private static String associationKey(Element association) {
        var role = association.getAttribute("targetRoleSingular");
        var target = association.getAttribute("target");
        return association.getAttribute("associationType") + ":" + (role.isBlank() ? target : role);
    }

    private static javax.xml.parsers.DocumentBuilder newBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void write(Document doc, Path target) throws TransformerException, IOException {
        var transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        try (var out = Files.newOutputStream(target)) {
            transformer.transform(new DOMSource(doc), new StreamResult(out));
        }
    }
}
