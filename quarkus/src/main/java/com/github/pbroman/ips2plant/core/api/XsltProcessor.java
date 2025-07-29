package com.github.pbroman.ips2plant.core.api;

import java.nio.file.Path;
import java.util.Map;

public interface XsltProcessor {

    /**
     * Processes an XSL transformation on an XML file with optional parameters.
     *
     * @param xslt the {@link Path} to the XSL file
     * @param xml the {@link Path} to the XML file
     * @param result the {@link Path} to the result file
     * @param stringParams a map of string stringParams
     */
    void process(Path xslt, Path xml, Path result, Map<String, String> stringParams);
}
