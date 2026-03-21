package com.github.pbroman.ips2plant.core;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class IpsFileCollector {

    private static final String IPS_FILE_REGEX = ".+\\.ips.+";

    public Map<String, File> collect(List<Path> dirPaths) {
        var result = new HashMap<String, File>();
        dirPaths.forEach(path -> {
            var dir = path.toFile();
            recursiveCollect(dir, dir, result);
        });
        return result;
    }

    private void recursiveCollect(File file, File root, Map<String, File> map) {
        if (file.isFile() && file.getName().matches(IPS_FILE_REGEX)) {
            var relativePath = root.toPath().relativize(file.toPath()).toString();
            var className = relativePath
                    .substring(0, relativePath.lastIndexOf('.'))
                    .replace(File.separatorChar, '.');
            map.put(className, file);
        } else if (file.isDirectory()) {
            for (File child : Objects.requireNonNull(file.listFiles())) {
                recursiveCollect(child, root, map);
            }
        }
    }
}