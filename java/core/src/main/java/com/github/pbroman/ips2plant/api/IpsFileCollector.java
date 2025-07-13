package com.github.pbroman.ips2plant.api;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface IpsFileCollector {

    /**
     * Collects ips files from any number of model paths and puts them in a map.
     * As map keys, the complete ips class name including package path (e.g. lodi.Lodi) must be used.
     *
     * @param dirPaths any number of ips model directory paths
     * @return a map with the result
     */
    Map<String, File> collect(List<Path> dirPaths);
}
