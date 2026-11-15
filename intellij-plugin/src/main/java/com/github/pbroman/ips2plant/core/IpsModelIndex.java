package com.github.pbroman.ips2plant.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
 * Parsed view of all policy component types in a set of model dirs. No IntelliJ dependencies.
 */
public class IpsModelIndex {

    private static final String POLICY_EXTENSION = ".ipspolicycmpttype";

    public record Assoc(String type, String target, String role, boolean constrain) {
    }

    public record OwnedAssoc(String owner, Assoc assoc) {
    }

    public record ClassInfo(String fqn, File file, boolean isAbstract, String supertype, List<Assoc> associations) {
    }

    private final Map<String, ClassInfo> classes = new LinkedHashMap<>();
    private final Set<String> inScope = new HashSet<>();
    private final Map<String, List<String>> subtypes = new LinkedHashMap<>();

    /**
     * @param allDirs   all known model dirs (selected + dependencies); used to resolve supertype chains
     * @param scopeDirs dirs whose classes may become nodes; classes found only in other dirs are out of scope
     */
    public static IpsModelIndex build(List<Path> allDirs, List<Path> scopeDirs) {
        var index = new IpsModelIndex();
        var collector = new IpsFileCollector();
        var scopeSet = new HashSet<>(scopeDirs);
        var ordered = new ArrayList<Path>(scopeDirs);
        allDirs.stream().filter(d -> !scopeSet.contains(d)).forEach(ordered::add);
        for (var dir : ordered) {
            boolean scoped = scopeSet.contains(dir);
            collector.collect(List.of(dir)).forEach((fqn, file) -> {
                if (!file.getName().endsWith(POLICY_EXTENSION) || index.classes.containsKey(fqn)) {
                    return;
                }
                parse(fqn, file).ifPresent(info -> {
                    index.classes.put(fqn, info);
                    if (scoped) {
                        index.inScope.add(fqn);
                    }
                });
            });
        }
        index.classes.values().forEach(info -> {
            if (info.supertype() != null) {
                index.subtypes.computeIfAbsent(info.supertype(), k -> new ArrayList<>()).add(info.fqn());
            }
        });
        return index;
    }

    public Optional<ClassInfo> get(String fqn) {
        return Optional.ofNullable(classes.get(fqn));
    }

    public Set<String> allClasses() {
        return java.util.Collections.unmodifiableSet(classes.keySet());
    }

    public boolean isInScope(String fqn) {
        return inScope.contains(fqn);
    }

    /**
     * Resolves a fully qualified or simple name; exact matches win over case-insensitive ones, and a tier with
     * more than one match is ambiguous. Empty if unknown or ambiguous.
     */
    public Optional<String> resolve(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        var trimmed = name.trim();
        if (classes.containsKey(trimmed)) {
            return Optional.of(trimmed);
        }
        var tiers = List.<java.util.function.Predicate<String>>of(
                fqn -> simpleName(fqn).equals(trimmed),
                fqn -> fqn.equalsIgnoreCase(trimmed),
                fqn -> simpleName(fqn).equalsIgnoreCase(trimmed));
        for (var tier : tiers) {
            var matches = classes.keySet().stream().filter(tier).toList();
            if (matches.size() == 1) {
                return Optional.of(matches.get(0));
            }
            if (matches.size() > 1) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /** Direct subtypes only. */
    public List<String> subtypesOf(String fqn) {
        return subtypes.getOrDefault(fqn, List.of());
    }

    /** Supertypes from nearest to farthest, excluding the class itself. Stops at unknown types and cycles. */
    public List<String> supertypeChain(String fqn) {
        var chain = new ArrayList<String>();
        var seen = new HashSet<String>();
        seen.add(fqn);
        var current = classes.get(fqn);
        while (current != null && current.supertype() != null && seen.add(current.supertype())) {
            chain.add(current.supertype());
            current = classes.get(current.supertype());
        }
        return chain;
    }

    /**
     * Supertypes referenced somewhere in the supertype chains of the given classes but missing from the index,
     * e.g. because they live in an unresolved dependency. Checks the whole chain, not just the direct supertype.
     */
    public List<String> unresolvedSupertypes(java.util.Collection<String> fqns) {
        return fqns.stream()
                .map(this::supertypeChain)
                .filter(chain -> !chain.isEmpty())
                .map(chain -> chain.get(chain.size() - 1))
                .filter(last -> !classes.containsKey(last))
                .distinct()
                .toList();
    }

    /** Own associations plus those of the whole supertype chain; a re-declared role keeps the most specific one. */
    public List<OwnedAssoc> effectiveAssociations(String fqn) {
        var result = new LinkedHashMap<String, OwnedAssoc>();
        var hierarchy = new ArrayList<String>();
        hierarchy.add(fqn);
        hierarchy.addAll(supertypeChain(fqn));
        for (var owner : hierarchy) {
            var info = classes.get(owner);
            if (info == null) {
                continue;
            }
            for (var assoc : info.associations()) {
                result.putIfAbsent(assoc.type() + ":" + assoc.role(), new OwnedAssoc(owner, assoc));
            }
        }
        return List.copyOf(result.values());
    }

    static String simpleName(String fqn) {
        return fqn.substring(fqn.lastIndexOf('.') + 1);
    }

    private static Optional<ClassInfo> parse(String fqn, File file) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            var root = factory.newDocumentBuilder().parse(file).getDocumentElement();
            var supertype = blankToNull(root.getAttribute("supertype"));
            var associations = new ArrayList<Assoc>();
            for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child instanceof Element e && e.getTagName().equals("Association")) {
                    var target = e.getAttribute("target");
                    if (target.isBlank()) {
                        continue;
                    }
                    var role = e.getAttribute("targetRoleSingular");
                    associations.add(new Assoc(
                            e.getAttribute("associationType"),
                            target,
                            role.isBlank() ? target : role,
                            "true".equals(e.getAttribute("constrain"))));
                }
            }
            return Optional.of(new ClassInfo(fqn, file, "true".equals(root.getAttribute("abstract")),
                    supertype, List.copyOf(associations)));
        } catch (IOException | SAXException | ParserConfigurationException e) {
            return Optional.empty();
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
