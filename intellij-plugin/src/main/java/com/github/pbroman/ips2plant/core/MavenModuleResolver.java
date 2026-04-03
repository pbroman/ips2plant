package com.github.pbroman.ips2plant.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Resolves the Maven module (groupId:artifactId) for IPS model files
 * by walking up the directory tree to find the nearest pom.xml.
 */
public class MavenModuleResolver {

    private static final Pattern GROUP_ID_PATTERN = Pattern.compile("<groupId>([^<]+)</groupId>");
    private static final Pattern ARTIFACT_ID_PATTERN = Pattern.compile("<artifactId>([^<]+)</artifactId>");

    private final Map<Path, String> cache = new HashMap<>();
    private final Map<Path, String> registeredModules = new HashMap<>();

    /**
     * Registers a known maven module for a directory (e.g. for dependency temp dirs).
     */
    public void registerModule(Path directory, String mavenModule) {
        registeredModules.put(directory.toAbsolutePath().normalize(), mavenModule);
    }

    /**
     * Builds a map of className -> "groupId:artifactId" for all entries in the given file map.
     */
    public Map<String, String> resolveAll(Map<String, File> ipsFiles) {
        var result = new HashMap<String, String>();
        for (var entry : ipsFiles.entrySet()) {
            var module = resolveModule(entry.getValue());
            if (module != null) {
                result.put(entry.getKey(), module);
            }
        }
        return result;
    }

    String resolveModule(File file) {
        // Check registered modules first (for dependency temp dirs)
        var filePath = file.toPath().toAbsolutePath().normalize();
        for (var entry : registeredModules.entrySet()) {
            if (filePath.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }

        // Walk up to find pom.xml
        var dir = file.isDirectory() ? file.toPath() : file.toPath().getParent();
        while (dir != null) {
            var pomFile = dir.resolve("pom.xml");
            if (Files.isRegularFile(pomFile)) {
                return getModuleFromPom(pomFile);
            }
            dir = dir.getParent();
        }
        return null;
    }

    private String getModuleFromPom(Path pomFile) {
        var normalized = pomFile.toAbsolutePath().normalize();
        if (cache.containsKey(normalized)) {
            return cache.get(normalized);
        }

        try {
            var content = Files.readString(normalized);
            var parentStripped = content.replaceAll("(?s)<parent>.*?</parent>", "");
            var depsIndex = parentStripped.indexOf("<dependencies");
            var header = depsIndex >= 0 ? parentStripped.substring(0, depsIndex) : parentStripped;

            var artifactId = extractFirst(ARTIFACT_ID_PATTERN, header);
            if (artifactId == null) {
                cache.put(normalized, null);
                return null;
            }

            var groupId = extractFirst(GROUP_ID_PATTERN, header);
            if (groupId == null) {
                var parentMatch = Pattern.compile("(?s)<parent>(.*?)</parent>").matcher(content);
                if (parentMatch.find()) {
                    groupId = extractFirst(GROUP_ID_PATTERN, parentMatch.group(1));
                }
            }

            var module = groupId != null ? groupId + ":" + artifactId : artifactId;
            cache.put(normalized, module);
            return module;
        } catch (IOException e) {
            cache.put(normalized, null);
            return null;
        }
    }

    private static String extractFirst(Pattern pattern, String text) {
        var matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : null;
    }
}