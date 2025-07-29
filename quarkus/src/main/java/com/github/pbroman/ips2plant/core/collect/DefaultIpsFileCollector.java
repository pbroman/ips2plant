package com.github.pbroman.ips2plant.core.collect;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.github.pbroman.ips2plant.core.api.IpsFileCollector;

public class DefaultIpsFileCollector implements IpsFileCollector {

    public static final String FILE_SEPARATOR_REGEX = "\\%s".formatted(File.separator);
    public static final String IPS_FILE_REGEX = ".+\\.ips.+";

    @Override
    public Map<String, File> collect(List<Path> dirPaths) {
        var result = new HashMap<String, File>();
        dirPaths.forEach(path -> {
            var dir = path.toFile();
            recursiveCollector(dir, dir, result);
        });

        return result;
    }

    private void recursiveCollector(File file, File root, Map<String, File> map) {
        if (file.isFile() && file.getName().matches(IPS_FILE_REGEX)) {
            var relativePath = root.toPath().relativize(file.toPath()).toString();
            var className = StringUtils.substringBeforeLast(relativePath, ".")
                    .replaceAll(FILE_SEPARATOR_REGEX, ".");
            map.put(className, file);

        } else if (file.listFiles() != null) {
            for (File child : Objects.requireNonNull(file.listFiles())) {
                recursiveCollector(child, root, map);
            }
        }
    }

}
