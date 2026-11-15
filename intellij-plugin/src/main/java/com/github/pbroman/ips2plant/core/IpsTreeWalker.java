package com.github.pbroman.ips2plant.core;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.pbroman.ips2plant.core.IpsModelIndex.OwnedAssoc;

/**
 * Collects descendants and/or ancestors of a root policy type. No IntelliJ dependencies.
 */
public class IpsTreeWalker {

    private static final Set<String> FORWARD_TYPES = Set.of("comp", "ass", "aggr");

    private record Edge(String from, String to) {
    }

    private final IpsModelIndex index;
    private final Map<String, List<OwnedAssoc>> effectiveCache = new HashMap<>();
    private final Set<String> nodes = new LinkedHashSet<>();
    private final Set<Edge> edges = new LinkedHashSet<>();
    private final Map<String, Set<String>> subLinks = new HashMap<>();
    private String root;
    private boolean includeAbstract;
    private boolean mergeSupertypes;

    public IpsTreeWalker(IpsModelIndex index) {
        this.index = index;
    }

    /**
     * @param depth association hops, null = unlimited; supertype hops cost nothing
     * @return fully qualified name to file for every class to render, including out-of-scope owners that carry
     *         an inherited association arrow; empty if the root is unknown or out of scope
     */
    public Map<String, File> walk(String rootFqn, boolean descendants, boolean ancestors,
                                  Integer depth, boolean includeAbstract) {
        return walk(rootFqn, descendants, ancestors, depth, includeAbstract, false);
    }

    /**
     * @param mergeSupertypes the caller merges inherited members into each returned class, so owners of inherited
     *                        associations are not needed as nodes; arrows are drawn from the reached class itself
     */
    public Map<String, File> walk(String rootFqn, boolean descendants, boolean ancestors,
                                  Integer depth, boolean includeAbstract, boolean mergeSupertypes) {
        this.root = rootFqn;
        this.includeAbstract = includeAbstract;
        this.mergeSupertypes = mergeSupertypes;
        nodes.clear();
        edges.clear();
        subLinks.clear();
        if (!index.isInScope(rootFqn)) {
            return Map.of();
        }
        int maxDepth = depth == null ? Integer.MAX_VALUE : depth;
        nodes.add(rootFqn);
        if (descendants) {
            walkDescendants(rootFqn, maxDepth);
        }
        if (ancestors) {
            walkAncestors(rootFqn, maxDepth);
        }
        int before;
        do {
            before = nodes.size();
            if (mergeSupertypes) {
                pruneSupertypeOnlyNodes(rootFqn);
            }
            if (!includeAbstract) {
                pruneAbstractLeaves(rootFqn);
            }
        } while (nodes.size() != before);
        var result = new LinkedHashMap<String, File>();
        for (var fqn : nodes) {
            index.get(fqn).ifPresent(info -> result.put(fqn, info.file()));
        }
        return result;
    }

    private void walkDescendants(String root, int maxDepth) {
        var visited = new HashSet<String>();
        visited.add(root);
        var queue = new ArrayDeque<Map.Entry<String, Integer>>();
        queue.add(Map.entry(root, 0));
        while (!queue.isEmpty()) {
            var current = queue.poll();
            if (current.getValue() >= maxDepth) {
                continue;
            }
            int next = current.getValue() + 1;
            for (var owned : effective(current.getKey())) {
                if (!FORWARD_TYPES.contains(owned.assoc().type())) {
                    continue;
                }
                var target = owned.assoc().target();
                if (!index.isInScope(target) || isDeadEndAbstract(target)) {
                    continue;
                }
                var from = mergeSupertypes ? current.getKey() : owned.owner();
                nodes.add(from);
                if (isRetargeted(target)) {
                    // the merger points this arrow at the concrete subtypes, so the abstract target is not drawn
                    for (var sub : concreteInScopeSubtypes(target)) {
                        nodes.add(sub);
                        edges.add(new Edge(from, sub));
                        if (visited.add(sub)) {
                            queue.add(Map.entry(sub, next));
                        }
                    }
                    continue;
                }
                nodes.add(target);
                edges.add(new Edge(from, target));
                if (visited.add(target)) {
                    queue.add(Map.entry(target, next));
                }
                // an association to a type can hold any of its subtypes, so reach the in-scope concrete ones too
                for (var sub : concreteInScopeSubtypes(target)) {
                    nodes.add(sub);
                    subLinks.computeIfAbsent(target, k -> new HashSet<>()).add(sub);
                    if (visited.add(sub)) {
                        queue.add(Map.entry(sub, next));
                    }
                }
            }
        }
    }

    private void walkAncestors(String root, int maxDepth) {
        var visited = new HashSet<String>();
        visited.add(root);
        var queue = new ArrayDeque<Map.Entry<String, Integer>>();
        queue.add(Map.entry(root, 0));
        while (!queue.isEmpty()) {
            var current = queue.poll();
            if (current.getValue() >= maxDepth) {
                continue;
            }
            int next = current.getValue() + 1;
            var child = current.getKey();
            var targets = new ArrayList<String>();
            targets.add(child);
            targets.addAll(index.supertypeChain(child));

            for (var parent : index.allClasses()) {
                if (!index.isInScope(parent) || isDeadEndAbstract(parent)) {
                    continue;
                }
                for (var owned : effective(parent)) {
                    if (!FORWARD_TYPES.contains(owned.assoc().type())
                            || !targets.contains(owned.assoc().target())
                            || !declaredHereOrOutOfScope(parent, owned)) {
                        continue;
                    }
                    addAncestorEdge(parent, owned, child);
                    if (visited.add(parent)) {
                        queue.add(Map.entry(parent, next));
                    }
                }
            }

            for (var owned : effective(child)) {
                if (!"reverseComp".equals(owned.assoc().type()) || !index.isInScope(owned.assoc().target())
                        || isDeadEndAbstract(owned.assoc().target())) {
                    continue;
                }
                var parent = owned.assoc().target();
                nodes.add(parent);
                for (var parentAssoc : effective(parent)) {
                    if (FORWARD_TYPES.contains(parentAssoc.assoc().type())
                            && targets.contains(parentAssoc.assoc().target())) {
                        addAncestorEdge(parent, parentAssoc, child);
                    }
                }
                if (visited.add(parent)) {
                    queue.add(Map.entry(parent, next));
                }
            }
        }
    }

    private void addAncestorEdge(String parent, OwnedAssoc owned, String child) {
        var target = owned.assoc().target();
        var from = mergeSupertypes ? parent : owned.owner();
        nodes.add(parent);
        nodes.add(from);
        if (isRetargeted(target) && concreteInScopeSubtypes(target).contains(child)) {
            edges.add(new Edge(from, child));
            nodes.add(child);
        } else {
            nodes.add(target);
            edges.add(new Edge(from, target));
        }
        if (!target.equals(child) && !isRetargeted(target)) {
            subLinks.computeIfAbsent(target, k -> new HashSet<>()).add(child);
        }
        if (!from.equals(parent)) {
            subLinks.computeIfAbsent(from, k -> new HashSet<>()).add(parent);
        }
    }

    /** An in-scope owner is its own parent; an out-of-scope owner makes its in-scope inheritors parents. */
    private boolean declaredHereOrOutOfScope(String parent, OwnedAssoc owned) {
        if (owned.owner().equals(parent)) {
            return true;
        }
        if (index.isInScope(owned.owner())) {
            return false;
        }
        for (var superType : index.supertypeChain(parent)) {
            if (index.isInScope(superType) && effective(superType).contains(owned)) {
                return false;
            }
        }
        return true;
    }

    /**
     * With merged supertypes, an arrow to an abstract class is drawn to its concrete in-scope subtypes instead
     * (see {@link IpsSupertypeMerger}), unless abstract classes are requested or the class is the root.
     */
    private boolean isRetargeted(String target) {
        return mergeSupertypes
                && !includeAbstract
                && !target.equals(root)
                && index.get(target).map(IpsModelIndex.ClassInfo::isAbstract).orElse(false)
                && !concreteInScopeSubtypes(target).isEmpty();
    }

    /** An abstract class without a concrete in-scope subtype can never be instantiated, so nothing leads through it. */
    private boolean isDeadEndAbstract(String fqn) {
        return !includeAbstract
                && !fqn.equals(root)
                && index.get(fqn).map(IpsModelIndex.ClassInfo::isAbstract).orElse(false)
                && concreteInScopeSubtypes(fqn).isEmpty();
    }

    private List<String> concreteInScopeSubtypes(String abstractType) {
        var result = new ArrayList<String>();
        var seen = new HashSet<String>();
        var stack = new ArrayDeque<String>();
        stack.push(abstractType);
        while (!stack.isEmpty()) {
            for (var sub : index.subtypesOf(stack.pop())) {
                if (!seen.add(sub)) {
                    continue;
                }
                stack.push(sub);
                if (index.isInScope(sub) && !index.get(sub).map(IpsModelIndex.ClassInfo::isAbstract).orElse(false)) {
                    result.add(sub);
                }
            }
        }
        return result;
    }

    /** With merged supertypes, a supertype shows up only if another class has an arrow to it. */
    private void pruneSupertypeOnlyNodes(String root) {
        for (var node : List.copyOf(nodes)) {
            if (node.equals(root)) {
                continue;
            }
            boolean hasSubtypeNode = nodes.stream()
                    .anyMatch(n -> !n.equals(node) && index.supertypeChain(n).contains(node));
            boolean isArrowTarget = edges.stream()
                    .anyMatch(e -> e.to().equals(node) && !e.from().equals(node) && nodes.contains(e.from()));
            if (hasSubtypeNode && !isArrowTarget) {
                nodes.remove(node);
            }
        }
    }

    private void pruneAbstractLeaves(String root) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (var node : List.copyOf(nodes)) {
                if (node.equals(root) || !index.get(node).map(IpsModelIndex.ClassInfo::isAbstract).orElse(false)) {
                    continue;
                }
                boolean hasOutgoing = edges.stream().anyMatch(e -> e.from().equals(node) && nodes.contains(e.to()))
                        || subLinks.getOrDefault(node, Set.of()).stream().anyMatch(nodes::contains);
                if (!hasOutgoing) {
                    nodes.remove(node);
                    changed = true;
                }
            }
        }
    }

    private List<OwnedAssoc> effective(String fqn) {
        return effectiveCache.computeIfAbsent(fqn, index::effectiveAssociations);
    }
}
