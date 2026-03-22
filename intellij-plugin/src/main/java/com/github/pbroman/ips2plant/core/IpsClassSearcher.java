package com.github.pbroman.ips2plant.core;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
